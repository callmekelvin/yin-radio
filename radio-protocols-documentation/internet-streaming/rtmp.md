### RTMP

**Overview**

Real-Time Messaging Protocol (RTMP) is Adobe's TCP-based protocol for streaming audio, video, and data over the internet. Originally designed for Flash Player, RTMP is still widely used for ingest (sending streams to servers) even as playback has shifted to HLS/DASH.

**Connection and Setup**

Phase 1: TCP connection + 3-way handshake (C0/C1/C2, S0/S1/S2)

Phase 2: AMF0 command exchange:
```
Client -> Server: connect
  app: "live"
  flashVer: "FMSc/1.0"
  tcUrl: "rtmp://server/live"
  audioCodecs: 0x0400  // AAC
  videoCodecs: 0x0080  // H.264

Server -> Client: _result
  code: "NetConnection.Connect.Success"

Client -> Server: createStream
Server -> Client: _result (returns streamId = 1)

Client -> Server: play("stream123")    // viewer
  OR
Client -> Server: publish("stream123", "live")  // broadcaster

Server -> Client: onStatus "NetStream.Play.Start"
[Audio/Video data flows on streamId 1]
```

**Protocol Stack**

```
+------------------+
|   AMF0 / AMF3    |  <-- Action Message Format (metadata/commands)
+------------------+
|   Chunk Stream   |  <-- Multiplexes messages into chunks
+------------------+
|   RTMP (TCP)     |  <-- Default port 1935
+------------------+
|      TCP         |
+------------------+
|       IP         |
+------------------+
```

**AMF0 and AMF3: Application Layer Serialization**

AMF (Action Message Format) is a binary serialization format used to encode commands, metadata, and data objects exchanged between the client and server. It operates at the **Application Layer (Layer 7)** of the OSI model.

AMF is fundamentally different from HTTP. While HTTP is a text-based request/response protocol with its own headers and methods, AMF is a **message payload format** carried inside RTMP's own message framing system. RTMP defines its own message types, chunking rules, and connection state machine. It does not use HTTP semantics, headers, or methods. The client and server communicate by sending RTMP messages with AMF-encoded payloads, not by issuing HTTP GET or POST requests.

**TCP Connection and Media Delivery**

RTMP operates over a **single, persistent TCP connection** (default port 1935) for the entire session. All messages—commands, metadata, audio, and video—are multiplexed over this one socket.

RTMP **does not** rely on TCP Keep-Alive to deliver media. Instead, it uses its own **Chunk Stream Layer** to fragment large messages (such as audio or video frames) into smaller chunks and interleave them over the single TCP connection.

To prevent middleboxes (such as NAT routers or firewalls) from dropping an idle connection, RTMP implements an **application-level keep-alive** mechanism using User Control Messages (Type 4), specifically `Ping` and `Pong`. When a server or client sends a `Ping` event, the peer must respond with a `Pong`. This is an **OSI Layer 7** mechanism and is distinct from both TCP Keep-Alive probes and the media chunking process.

**Handshake (3-step)**

```
Client -> Server: C0 + C1 (1537 bytes total)
  C0: Version byte (0x03)
  C1: 1536 bytes random + epoch timestamp

Server -> Client: S0 + S1 + S2
  S0: Version byte (0x03)
  S1: 1536 bytes (server epoch + random)
  S2: Copy of C1

Client -> Server: C2
  C2: Copy of S1

Connection established.
```

**Chunk Format**

```
Basic Header (1-3 bytes):
  +--------+--------+--------+
  |Fmt(2)  |CS ID(6)| [Extended CS ID]  |
  +--------+--------+--------+

  Fmt:   Chunk message header type (0=full, 1=relative, 2=8-byte, 3=0-byte)
  CS ID: Chunk stream identifier (2-63 inline, 64-319: +1 byte, 64-65599: +2 bytes)

Message Header (variable):
  Type 0 (11 bytes): timestamp(3) + messageLength(3) + messageTypeId(1) + messageStreamId(4)
  Type 1 (7 bytes):  timestampDelta(3) + messageLength(3) + messageTypeId(1)
  Type 2 (3 bytes):  timestampDelta(3)
  Type 3 (0 bytes):  Continuation of previous chunk

Extended Timestamp (4 bytes, present if timestamp == 0xFFFFFF)

Payload (up to chunkSize bytes, default 128)
```

**Message Types**

| Type ID | Name | Description |
|---------|------|-------------|
| 1 | Set Chunk Size | Changes max payload size |
| 2 | Abort Message | Discards partial message |
| 3 | Acknowledgement | Sequence number ACK |
| 4 | User Control | Stream begin/end, buffer empty/full |
| 5 | Window Ack Size | Set window size for ACKs |
| 6 | Set Peer Bandwidth | Limit downstream bandwidth |
| 8 | Audio Message | AAC/MP3/Speex audio frame |
| 9 | Video Message | H.264/VP6 video frame |
| 15 | AMF3 Data | Metadata (@setDataFrame) |
| 16 | AMF3 Shared Object | Shared object update |
| 17 | AMF3 Command | Remote procedure call |
| 18 | AMF0 Data | Metadata (@setDataFrame) |
| 20 | AMF0 Command | connect/publish/play calls |

**Chunk Stream Layer Mechanics**

The Chunk Stream Layer is responsible for breaking large RTMP messages into smaller pieces called "chunks" so they can be multiplexed over a single TCP connection.

When a sender needs to transmit a message (e.g., an audio frame or a command), the Chunk Stream Layer splits the message payload into chunks. Each chunk carries a piece of the payload along with a header that identifies which message it belongs to. This allows the sender to interleave chunks from different messages, ensuring that a large video frame does not block a small audio frame or a time-sensitive command.

**Chunk Basic Header**

The first part of every chunk is the Basic Header (1 to 3 bytes):
- `Fmt` (2 bits): Determines the format of the Message Header that follows.
- `CS ID` (6 bits): The Chunk Stream Identifier. Values 2–63 are encoded directly. Values 64–319 and 64–65599 require additional bytes.

**Message Header Formats**

The `Fmt` field in the Basic Header selects one of four Message Header types:

| Fmt | Type | Size | Description |
|-----|------|------|-------------|
| 0 | Full | 11 bytes | Contains full timestamp, message length, message type ID, and message stream ID. Used for the first chunk of a new message. |
| 1 | Relative | 7 bytes | Contains timestamp delta, message length, and message type ID. Assumes the same message stream ID as the previous chunk on this chunk stream. |
| 2 | 8-byte | 3 bytes | Contains only the timestamp delta. Assumes the same message length and type ID as the previous chunk. |
| 3 | 0-byte | 0 bytes | Contains no header fields. It is a continuation of the previous chunk on this chunk stream. |

**Reassembling Messages**

The receiver uses the `CS ID` to track the state of each chunk stream. When it receives a Type 0 header, it starts a new message and records the timestamp, length, type, and stream ID. For subsequent chunks with the same `CS ID`, it uses the appropriate header fields (or lack thereof) to append the payload bytes. Once the accumulated payload size equals the message length, the message is complete and passed up to the message handler.

**Chunk Size**

The default maximum chunk payload size is 128 bytes. A sender can change this by sending a `Set Chunk Size` message (Type 1). Increasing the chunk size reduces overhead but may increase latency for multiplexing.

**Audio Message Format (Type 8)**

```
Audio Tag Header (1-2 bytes):
  +--------+--------+--------+
  |Format(4)|Rate(2)|Size(1)|Type(1)| [AACPacketType(8)] |
  +--------+--------+--------+

  Format:  10 = AAC
  Rate:    3 = 44 kHz
  Size:    1 = 16-bit
  Type:    1 = Stereo

  AAC Packet Type (if Format=10):
    0 = AAC sequence header (AudioSpecificConfig)
    1 = AAC raw frame

AAC Sequence Header (AudioSpecificConfig):
  +--------+--------+--------+--------+
  | AudioObjectType(5) | SamplingFreqIndex(4) | ChannelConfig(4) |
  +--------+--------+--------+--------+
  | [FrameLengthFlag(1)] | [DependsOnCoreCoder(1)] | ... |
  +--------+--------+--------+--------+

AAC Raw Frame:
  +--------+--------+--------+--------+
  | ADTS-frame or raw AAC data          |
  +--------+--------+--------+--------+
```

**FLV Container on Wire**

RTMP audio/video is essentially FLV format streamed in real-time:

```
FLV Tag Header (11 bytes):
  +--------+--------+--------+--------+
  | TagType(1) | DataSize(3) | Timestamp(3) | TimestampExt(1) | StreamID(3) |
  +--------+--------+--------+--------+

  TagType: 8 = audio, 9 = video, 18 = script data (AMF metadata)
```

**AMF0 Data Parsing (Metadata)**

```
@setDataFrame("onMetaData", {
  duration: 0,
  filesize: 0,
  audiodatarate: 128,
  audiocodecid: 10,        // 10 = AAC
  stereo: true,
  audiosamplerate: 44100,
  encoder: "Lavf58.76.100"
})

AMF0 Type Markers:
  0x00 = Number (8-byte double)
  0x01 = Boolean (1 byte)
  0x02 = String (2-byte length + UTF-8)
  0x03 = Object (key-value pairs, terminated by 0x000009)
  0x05 = Null
  0x08 = ECMA Array (4-byte count + key-value pairs)
  0x0A = Strict Array
  0x0B = Date
```


**Playback Flow (Viewer)**

When a client wants to play an RTMP stream, it follows a strict handshake and command sequence:

**Phase 1: RTMP Handshake**
1. Client sends `C0` (1 byte, version 0x03) and `C1` (1536 bytes with epoch timestamp and random data).
2. Server responds with `S0` (version), `S1` (server epoch + random), and `S2` (echo of C1).
3. Client sends `C2` (echo of S1).
4. The connection is now established at the RTMP level.

**Phase 2: NetConnection Establishment**
1. Client sends an AMF0 `connect` command with parameters:
   - `app`: The application name (e.g., `"live"`).
   - `flashVer`: Flash version string.
   - `tcUrl`: The RTMP URL (e.g., `"rtmp://server/live"`).
   - `audioCodecs` / `videoCodecs`: Bitmasks indicating supported codecs.
2. Server responds with `_result` and status `NetConnection.Connect.Success`.

**Phase 3: Stream Creation**
1. Client sends a `createStream` command.
2. Server responds with `_result` containing a `streamId` (e.g., `1`).

**Phase 4: Playback Request**
1. Client sends a `play("stream123")` command on the newly created stream.
2. Server responds with an `onStatus` command carrying `NetStream.Play.Start`.
3. The server may also send a User Control Message (Type 4) indicating `StreamBegin` for that stream ID.

**Phase 5: Media Delivery**
- The server begins sending audio messages (Type 8), video messages (Type 9), and metadata (Type 18/15) on the negotiated `streamId`.
- The client demuxes the FLV-formatted payloads, decodes the audio/video, and renders them.

**Audio-Only Streams**

For internet radio or audio-only broadcasts, the RTMP stream contains no video messages. The absence of Type 9 (Video) messages signals to the client that this is an audio-only stream.

**Audio Message Structure (Type 8)**

Every audio message begins with a 1- or 2-byte header:
- `Format` (4 bits): Codec identifier. `10` = AAC.
- `Rate` (2 bits): Sampling rate. `3` = 44 kHz.
- `Size` (1 bit): Sample size. `1` = 16-bit.
- `Type` (1 bit): Channels. `1` = Stereo.

If the format is AAC (`10`), an additional byte follows:
- `AAC Packet Type`: 
  - `0` = AAC sequence header (AudioSpecificConfig). This must be sent before any raw audio frames so the decoder can initialize.
  - `1` = AAC raw frame. The actual compressed audio data follows.

**Sequence Header**

The AAC sequence header contains the `AudioSpecificConfig` (ASC), which includes:
- `AudioObjectType` (5 bits): e.g., `2` for AAC-LC.
- `SamplingFrequencyIndex` (4 bits): Maps to the sample rate.
- `ChannelConfiguration` (4 bits): Number of audio channels.

After the sequence header is sent, the broadcaster continuously sends Type 8 messages with `AAC Packet Type = 1` containing raw AAC frames. The client buffers and decodes these frames to produce PCM audio for playback.


---


