### SRT

**Overview**

Secure Reliable Transport (SRT) is an open-source protocol developed by Haivision. It extends UDP with ARQ (Automatic Repeat Request) for packet recovery, AES-128/256 encryption, and latency control. Increasingly adopted for professional broadcast contribution and low-latency streaming.

**How It Works**

SRT is an application-layer (Layer 7) protocol that rides on top of UDP (Layer 4). It takes an unreliable datagram transport and adds reliability, security, and timing control without falling back to TCP's head-of-line blocking. The core innovation is **TSBPD (Time-Stamped Based Packet Delivery)**: every packet is stamped with a precise transmission time, and the receiver uses these timestamps to reconstruct the original packet timing before handing data to the decoder. This smooths out network jitter and ensures consistent playback, even when packets arrive early or late. Reliability is achieved through a selective ARQ mechanism: the receiver only asks for missing packets (NAK), rather than acknowledging every packet (ACK), keeping overhead low.

**OSI Model Position**

| Protocol | OSI Layer | Notes |
|----------|-----------|-------|
| **SRT** | **Application (Layer 7)** | Adds reliability, encryption, and latency control on top of UDP. |
| **UDP** | **Transport (Layer 4)** | Carries SRT packets; provides port addressing and checksums. |

**Key Features**

| Feature | Implementation |
|---------|----------------|
| Packet Recovery | ACK, NAK, periodic ACK (every 10ms), lightweight NAK |
| Latency Control | Fixed receiver buffer (default 120ms), adapts to network |
| Encryption | AES-128 or AES-256 in CTR mode |
| Multiplexing | Stream ID for multiple logical streams over one connection |
| Congestion Control | Live mode (fixed bitrate) / File mode (max throughput) |

**Connection Modes**

SRT supports three connection modes to cover different network topologies:

| Mode | Role | Use Case |
|------|------|----------|
| **Caller** | Initiates outbound connection | Client behind NAT/Firewall connecting to a known server |
| **Listener** | Waits for inbound connection | Server with a public IP accepting streams |
| **Rendezvous** | Both peers initiate simultaneously | Two endpoints both behind NAT; uses UDP hole punching |

In Rendezvous mode, both peers attempt to open a connection to each other at the same time. Since both have recently sent an outbound UDP packet, their respective NATs keep a temporary mapping open, allowing the handshake to succeed without port forwarding.

**SRT Packet Structure**

```
+-------------------------------------------------------------+
| SRT Header (16 bytes)                                       |
+-------------------------------------------------------------+
| Timestamp (4 bytes)     | Destination Socket ID (4 bytes)   |
+-------------------------+-----------------------------------+
| Packet Sequence Number (4)  | Message Number (4)            |
+-----------------------------+-------------------------------+
| Payload Type (4 bits) | ... | Flags/Control Info (28 bits)  |
+-----------------------+-----+-------------------------------+
| Payload (variable, encrypted if configured)                 |
+-------------------------------------------------------------+
| HMAC/Integrity (optional, for control packets)              |
+-------------------------------------------------------------+

Timestamp: Microseconds since connection start (wraps at 2^32)
Sequence:  31-bit packet sequence (modulo 2^31)
Message:   31-bit message sequence (fragments share message number)

Payload Types:
  0 = DATA (audio/video payload)
  1 = CONTROL (ACK, NAK, Handshake, etc.)
```

**Handshake Phase**

```
Phase 1: Caller -> Listener (UDP)
  INDUCTION packet:
    Version: 4
    Type:    0xFFFFFFFD (SRT magic)
    Flags:   Request SRT extensions

Phase 2: Listener -> Caller
  CONCLUSION packet:
    Version: 5
    Crypto:  AES-128/256 or none
    Latency: Configured receiver buffer (e.g., 120)

Phase 3: Caller -> Listener
  CONCLUSION ACK:
    Confirms parameters
    Exchange of Stream ID (if multiplexing)

Data phase begins.
```

**Control Packet Types**

SRT uses separate control packets for session management and quality control:

| Packet Type | Direction | Purpose |
|-------------|-----------|---------|
| **Handshake** | Bidirectional | Establishes connection version, crypto, latency, and Stream ID |
| **ACK** | Receiver -> Sender | Full acknowledgment; reports highest contiguous received sequence and RTT |
| **ACKACK** | Sender -> Receiver | Acknowledges the ACK; used for precise RTT calculation |
| **NAK** | Receiver -> Sender | Negative acknowledgment; requests retransmission of missing sequences |
| **Keep-alive** | Bidirectional | Sent periodically to maintain NAT binding and detect dead peers |
| **Shutdown** | Either | Gracefully terminates the session |
| **Drop Request** | Receiver -> Sender | Signals that a specific message has exceeded the latency window and should be skipped |

**ARQ Mechanism**

```
Sender Side:
  +--------+    +--------+    +--------+    +--------+
  | Seq 1  | -> | Seq 2  | -> | Seq 3  | -> | Seq 4  |
  +--------+    +--------+    +--------+    +--------+
       |                              X (Seq 3 lost)
       |                              |
  Store in send buffer (latency * 1.5 duration)

Receiver Side:
  Receives: Seq 1, Seq 2, Seq 4
  Detects gap: Seq 3 missing
  Sends NAK with range [3, 3]

Sender (upon NAK):
  Retransmits Seq 3 from buffer
  If buffer expired (past latency window), drop

ACK Feedback (every 10ms):
  Receiver sends ACK with highest contiguous received sequence
  Sender trims send buffer up to ACKed sequence
```

**Latency, Buffering & Jitter Handling**

SRT's performance depends on a configurable latency window (default **120 ms**):

- **Send Buffer**: Holds transmitted packets for a duration of roughly `latency * 1.5`. This gives the receiver enough time to detect a loss and request a retransmission before the sender discards the data.
- **Receive Buffer**: Reorders out-of-sequence packets and delays delivery using **TSBPD**. The receiver waits until the packet's timestamp matches the target playout time, smoothing jitter.
- **Trade-off**: A larger latency window tolerates more network jitter and allows more time for retransmissions, but increases end-to-end delay. A smaller window reduces latency but risks unrecoverable packet loss under poor network conditions.

---
