### RTP / RTSP

**Overview**

Real-time Transport Protocol (RTP) and its control companion RTCP, combined with the session setup protocol RTSP, form the IETF standard for real-time media streaming. Widely used in IP cameras, surveillance systems, and some legacy internet radio systems.

**How It Works**

RTSP acts like a remote control: it negotiates the session over TCP (port 554) but does not carry the media itself. Once the client and server agree on codecs and ports via an SDP exchange, the actual audio or video data flows using RTP. By default, RTP uses UDP for low-latency delivery, with RTCP running on the next port up to provide quality-of-service feedback, packet-loss statistics, and timing data. However, RTP can also be tunnelled over TCP (RTP/AVP/TCP) when UDP is blocked by firewalls or NAT, trading slightly higher latency for better traversability.

**Lifecycle Summary**

1. **Negotiate** – Client uses `DESCRIBE` to fetch an SDP file defining the stream's codecs, payload types, and clock rates.
2. **Bind** – Client uses `SETUP` to propose local UDP ports; the server confirms and issues a `Session` ID.
3. **Play** – Client sends `PLAY`; the server begins transmitting RTP packets to the bound UDP port.
4. **Monitor** – RTP delivers sequenced media while RTCP sends periodic sender/receiver reports for QoS and sync.
5. **End** – Client sends `TEARDOWN`; the server stops the RTP stream and closes the session.

**OSI Model Position**

| Protocol | OSI Layer | Notes |
|----------|-----------|-------|
| **RTSP** | **Application (Layer 7)** | Operates like HTTP — sends text commands to control the session. Carries no media data. |
| **RTP** | **Application (Layer 7)** | Handles sequencing and timing, but rides on top of UDP (Layer 4). |
| **RTCP** | **Application (Layer 7)** | Same layer as RTP. Uses the adjacent UDP port for control and monitoring. |
| **SDP** | **Application (Layer 7)** | Session description format exchanged during RTSP `DESCRIBE`. |

**Connection and Setup**

Full RTSP session lifecycle:
```
Step 1: OPTIONS (preflight) — RTSP over TCP
  Client -> Server (TCP 554):
    OPTIONS rtsp://server/stream RTSP/1.0
    CSeq: 1

  Server -> Client (TCP 554):
    RTSP/1.0 200 OK
    Public: DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE

Step 2: DESCRIBE — RTSP over TCP
  Client -> Server (TCP 554):
    DESCRIBE rtsp://server/stream RTSP/1.0
    CSeq: 2
    Accept: application/sdp

  Server -> Client (TCP 554):
    RTSP/1.0 200 OK
    Content-Type: application/sdp
    [SDP body with codec info, RTP port]

Step 3: SETUP — RTSP over TCP
  Client -> Server (TCP 554):
    SETUP rtsp://server/stream/trackID=1 RTSP/1.0
    CSeq: 3
    Transport: RTP/AVP;unicast;client_port=5004-5005

  Server -> Client (TCP 554):
    RTSP/1.0 200 OK
    Session: 12345678
    Transport: RTP/AVP;unicast;client_port=5004-5005;server_port=6004-6005

Step 4: PLAY — RTSP over TCP
  Client -> Server (TCP 554):
    PLAY rtsp://server/stream RTSP/1.0
    CSeq: 4
    Session: 12345678

  Server -> Client (TCP 554):
    RTSP/1.0 200 OK
    RTP-Info: url=rtsp://.../trackID=1;seq=12345;rtptime=0

Step 5: Data transfer — RTP/RTCP over UDP
  Server -> Client (UDP 5004): RTP audio packets
  Bidirectional (UDP 5005): RTCP Sender/Receiver Reports

Step 6: TEARDOWN — RTSP over TCP
  Client -> Server (TCP 554):
    TEARDOWN rtsp://server/stream RTSP/1.0
    CSeq: 5
    Session: 12345678

  Server -> Client (TCP 554):
    RTSP/1.0 200 OK
    [RTP stream stops]
```

**Protocol Relationships**

```
+-----------------------------------------------------+
|                    RTSP (TCP 554)                   |
|  DESCRIBE -> SDP    |   SETUP -> Transport          |
|  PLAY -> Stream     |   PAUSE / TEARDOWN            |
+-----------------------------------------------------+
                          |
          +---------------+---------------+
          v                               v
+------------------+            +------------------+
| RTP (UDP)        |            | RTCP (UDP)       |
| Media packets    |            | QoS feedback     |
| Port: even (e.g.,|            | Port: even+1     |
| 5004)            |            | (e.g., 5005)     |
+------------------+            +------------------+
```

**RTP Packet Structure**

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|V=2|P|X|  CC   |M|     PT      |       Sequence Number         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           Timestamp                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|           Synchronization Source (SSRC) identifier            |
+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
|            Contributing Source (CSRC) identifiers             |
|                             ....                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|              Payload (audio frame / codec data)               |
|                             ....                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

Header Fields:
  V (2 bits):     Version (always 2)
  P (1 bit):      Padding
  X (1 bit):      Extension present
  CC (4 bits):    CSRC count
  M (1 bit):      Marker bit (set for first packet after silence)
  PT (7 bits):    Payload Type (see table below)
  Sequence:       16-bit incremental counter
  Timestamp:      32-bit, units depend on clock rate
  SSRC:           32-bit random source identifier

Common Audio Payload Types:
  PT 0:  PCMU (G.711 mu-law)   - Clock: 8000 Hz
  PT 3:  GSM                    - Clock: 8000 Hz
  PT 4:  G.723                 - Clock: 8000 Hz
  PT 8:  PCMA (G.711 A-law)    - Clock: 8000 Hz
  PT 9:  G722                  - Clock: 8000 Hz
  PT 14: MP3                   - Clock: 90000 Hz
  PT 15: G728                  - Clock: 8000 Hz
  PT 18: G729                  - Clock: 8000 Hz
  PT 96-127: Dynamic (defined in SDP, e.g., AAC, Opus, Vorbis)
```

**SDP (Session Description Protocol) Example**

```
v=0
o=- 0 0 IN IP4 127.0.0.1
s=Radio Stream
t=0 0
m=audio 5004 RTP/AVP 96       <-- Media type, port, profile, payload type
a=rtpmap:96 AAC/48000/2        <-- Payload 96 = AAC, 48kHz, stereo
a=fmtp:96 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3
a=control:trackID=1            <-- RTSP control URL
```

**RTSP Session Flow**

```
Client -> Server:
  DESCRIBE rtsp://radio.example.com/stream RTSP/1.0
  CSeq: 1
  Accept: application/sdp

Server -> Client:
  RTSP/1.0 200 OK
  CSeq: 1
  Content-Type: application/sdp
  Content-Length: 256

  [SDP body describing streams]

Client -> Server:
  SETUP rtsp://radio.example.com/stream/trackID=1 RTSP/1.0
  CSeq: 2
  Transport: RTP/AVP;unicast;client_port=5004-5005

Server -> Client:
  RTSP/1.0 200 OK
  CSeq: 2
  Session: 12345678
  Transport: RTP/AVP;unicast;client_port=5004-5005;server_port=6004-6005

Client -> Server:
  PLAY rtsp://radio.example.com/stream RTSP/1.0
  CSeq: 3
  Session: 12345678

Server -> Client:
  RTSP/1.0 200 OK
  CSeq: 3
  Session: 12345678
  RTP-Info: url=rtsp://.../trackID=1;seq=12345;rtptime=0

  [Server now sends RTP packets to client's UDP port 5004]
  [RTCP sender reports to client's UDP port 5005]
```

**AAC over RTP (RFC 3640 - LATM)**

```
RTP Payload for AAC:
+------------------+------------------+------------------+
| AU-headers-length| AU-header (1..N) | AAC Audio Data   |
| (2 bytes)        | variable         | (variable)       |
+------------------+------------------+------------------+

AU-header per access unit:
  +--------+--------+--------+
  | AU-size (13 bits) | AU-index (3 bits) |
  +--------+--------+--------+
```


---

