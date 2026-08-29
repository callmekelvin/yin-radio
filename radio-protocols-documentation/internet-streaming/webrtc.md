### WebRTC

**Overview**

WebRTC is a collection of protocols and APIs enabling peer-to-peer real-time communication in browsers without plugins. While primarily designed for video conferencing, WebRTC's audio stack (Opus codec, jitter buffer, AEC) is increasingly used for low-latency interactive broadcasting.

**OSI Model Mapping**

WebRTC components map to the OSI model as follows:

```
+------------------+--------------------------------------------------+
| OSI Layer        | WebRTC Components                                |
+------------------+--------------------------------------------------+
| L7 Application   | JavaScript API, Signaling Channel                |
| L6 Presentation  | SDP (Session Description Protocol)               |
| L5 Session       | DTLS Handshake, ICE State Machine                |
| L4 Transport     | SRTP, SCTP (DataChannel), STUN/TURN over UDP     |
| L3 Network       | IP Routing, NAT Traversal                        |
| L2/L1            | Physical/Data Link (Ethernet, WiFi)              |
+------------------+--------------------------------------------------+
```

**Connection and Setup**

WebRTC establishes a peer-to-peer connection in three conceptual stages:
1. **Signaling:** Exchange session metadata (codec capabilities, network candidates) via an out-of-band channel.
2. **ICE:** Discover and test network paths to find a valid route through NATs and firewalls.
3. **DTLS/SRTP:** Authenticate the peer and negotiate encryption keys for secure media transfer.

Phase 1: Signaling (out-of-band)

The WebRTC specification does not define the signaling transport. Its sole purpose is to exchange session metadata so peers know *what* to send and *where* to send it.

```
Peer A -> Signaling Server: SDP Offer
Signaling Server -> Peer B: Forward offer
Peer B -> Signaling Server: SDP Answer
Signaling Server -> Peer A: Forward answer

[ICE candidates exchanged concurrently or inline]
```

Signaling State Machine:
```
stable --(createOffer)--> have-local-offer --(receive answer)--> stable
stable --(receive offer)--> have-remote-offer --(createAnswer)--> stable
```

Candidate Exchange:
- Vanilla ICE: Candidates bundled within the Offer/Answer.
- Trickle ICE: Candidates sent asynchronously as discovered.

Phase 2: ICE Connectivity Establishment

Because most endpoints are behind NATs, ICE gathers candidates (local IPs, STUN-reflexive addresses, and TURN relays) and tests pairs to locate a working route.

Candidate Types (preference order):
```
1. Host:        Direct local IP connection
2. Server Reflexive (srflx): Public IP via STUN
3. Peer Reflexive (prflx): Discovered during ICE checks
4. Relay:       TURN server relay
```

ICE Checklist:
- Local and remote candidates form pairs.
- Priority is calculated per RFC 8445:
  `priority = (2^24)*(type_pref) + (2^8)*(local_pref) + (256 - component_ID)`
- STUN Binding Requests validate each pair.

TURN Allocation:
```
Client -> TURN Server: Allocate Request
TURN Server -> Client: Allocate Response (relay address)
Client -> TURN Server: CreatePermission (peer IP)
Client -> TURN Server: Refresh Request (keepalive)
```

ICE Roles:
- `ICE-CONTROLLING`: Nominates the candidate pair (`use-candidate` attribute).
- `ICE-CONTROLLED`: Accepts nomination.

Consent Freshness (RFC 7675):
- Periodic STUN Binding Requests maintain NAT bindings and permissions.

Phase 3: DTLS Handshake

Once a candidate pair is nominated, DTLS secures the UDP path and authenticates peers via the certificate fingerprints exchanged in the SDP.

Executed over the nominated UDP candidate pair.

```
Peer A -> Peer B: ClientHello (+ use_srtp extension)
Peer B -> Peer A: ServerHello + Certificate + ServerHelloDone
Peer A -> Peer B: ClientKeyExchange + ChangeCipherSpec + Finished
Peer B -> Peer A: ChangeCipherSpec + Finished
```

Authentication:
- Peers verify the DTLS certificate fingerprint advertised in SDP:
  `a=fingerprint:sha-256 <hash>`

Role Determination:
- `a=setup:actpass`  : Can initiate or accept.
- `a=setup:active`   : DTLS client (initiates handshake).
- `a=setup:passive`  : DTLS server (waits for handshake).

Phase 4: SRTP Key Derivation and Media Flow

With DTLS complete, the derived SRTP keys encrypt media end-to-end. The application layer never sees these keys.

Key Export:
- DTLS Exporter derives the SRTP Master Key and Master Salt.

Key Derivation:
```
SRTP Master Key + Master Salt
         |
         v
+----------------+----------------+
| SRTP Encryption Key (AES-128)   |
| SRTP Authentication Key (HMAC)  |
| SRTP Salt                       |
+----------------+----------------+
```

Crypto Suites:
- `SRTP_AES128_CM_HMAC_SHA1_80`
- `SRTP_AEAD_AES_128_GCM`

Media is encrypted via SRTP. RTCP is multiplexed on the same port (`a=rtcp-mux`).

RTCPeerConnection State Machine:
```
new -> connecting -> connected -> disconnected -> failed
 |                    |              |
 |                    +--------------+-> closed
 +-> closed
```

**NAT Traversal**

NAT Type Impact on Direct P2P:
```
NAT Type              | Direct P2P Possible?
----------------------|---------------------
Full Cone             | Yes
Restricted Cone       | Yes
Port Restricted Cone  | Yes
Symmetric             | No (requires TURN relay)
```

Symmetric NATs create unpredictable port mappings per destination, preventing direct peer connectivity without a TURN relay.

**SDP Offer/Answer for Audio**

```
// SDP Offer (from caller)
v=0
o=- 1234567890 2 IN IP4 127.0.0.1
s=-
t=0 0
a=group:BUNDLE 0
a=msid-semantic: WMS stream1

m=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 0 8 106 105 13 110 112 113 126
  // 111 = Opus (preferred)
  // 103 = ISAC/16000
  // 104 = ISAC/32000
  // 9 = G722
  // 0 = PCMU
  // 8 = PCMA

c=IN IP4 0.0.0.0
a=rtcp:9 IN IP4 0.0.0.0
a=ice-ufrag:abcd1234
a=ice-pwd:xyz789abcdef
a=fingerprint:sha-256 00:01:02:...:FF
a=setup:actpass
a=mid:0
a=sendrecv
a=rtcp-mux
a=rtpmap:111 opus/48000/2
a=rtcp-fb:111 transport-cc
a=fmtp:111 minptime=10;useinbandfec=1
a=rtpmap:103 ISAC/16000
a=rtpmap:104 ISAC/32000
a=ssrc:123456789 cname:user@example.com
a=ssrc:123456789 msid:stream1 audioTrack
a=ssrc:123456789 mslabel:stream1
a=ssrc:123456789 label:audioTrack
```

**SRTP Packet**

```
RTP Header (12 bytes, same as standard RTP):
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|V=2|P|X|  CC   |M|     PT      |       Sequence Number         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           Timestamp                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|           Synchronization Source (SSRC) identifier            |
+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
|                        Encrypted Payload                        |
|                   (AES in Counter Mode)                         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        Authentication Tag                       |
|                       (HMAC-SHA1, 10 bytes)                     |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

Key Derivation (from DTLS-SRTP):
  SRTP Master Key + Master Salt ->
    SRTP Encryption Key (AES-128)
    SRTP Authentication Key (HMAC-SHA1)
    SRTP Salt
```

**Security Model**

- End-to-End Encryption: SRTP keys are derived via DTLS and never exposed to the application layer or signaling server.
- Identity Verification: `a=fingerprint` in SDP authenticates the DTLS certificate.
- Consent: ICE consent freshness (RFC 7675) prevents traffic injection to inactive peers.

**Opus RTP Payload**

```
Opus Frame in RTP:
  RTP Header (12 bytes)
  +--------------------------------------------------+
  | Opus TOC byte (Table of Contents)                  |
  |   Config (5 bits) + Stereo (1 bit) + Frames (2)   |
  +--------------------------------------------------+
  | Frame 1 data (variable)                            |
  +--------------------------------------------------+
  | [Frame 2 data] (if multiframe)                     |
  +--------------------------------------------------+
  | [Padding]                                          |
  +--------------------------------------------------+

Opus Config values (5 bits):
  0..11:  SILK-only modes (NB, MB, WB)
  12..15: Hybrid modes (SWB, FB)
  16..31: CELT-only modes (NB..FB)
```

---
