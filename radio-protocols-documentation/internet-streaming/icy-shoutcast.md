### ICY / SHOUTcast

**Overview**

ICY (I Can Yell) is the informal name for the HTTP-based streaming protocol popularized by Nullsoft's SHOUTcast server. It extends standard HTTP with custom headers for stream metadata delivery. SHOUTcast remains the dominant protocol for MP3 and AAC internet radio streams, supported by virtually all media players and browsers.

> **Related:** For the open-source server that implements and extends this protocol, see [Icecast](./icecast.md).

**Transport Stack**

```
+--------------------------------------------------+
|                 Audio Payload                      |
|         (MP3 frames / AAC ADTS frames)            |
+--------------------------------------------------+
|              ICY Inline Metadata                   |
|         (StreamTitle, StreamUrl, etc.)            |
+--------------------------------------------------+
|              HTTP/1.0 or HTTP/1.1                  |
+--------------------------------------------------+
|                    TCP                             |
+--------------------------------------------------+
|                    IP                              |
+--------------------------------------------------+
```

**Connection Handshake**

The client initiates a standard HTTP GET request. The server responds with `200 OK` and a continuous stream of audio data:

```
Client -> Server:
  GET /stream HTTP/1.0
  Host: radio.example.com
  User-Agent: Mozilla/5.0
  Icy-MetaData: 1        <-- Client requests metadata
  Accept: */*

Server -> Client:
  HTTP/1.0 200 OK
  Content-Type: audio/mpeg
  icy-br: 128            <-- Bitrate in kbps
  icy-genre: Jazz
  icy-name: Smooth Jazz Radio
  icy-url: https://example.com
  icy-pub: 1
  icy-metaint: 8192      <-- Metadata interval in bytes
  Server: Icecast 2.4.4

  [8192 bytes of MP3 audio]
  [metadata block]
  [8192 bytes of MP3 audio]
  [metadata block]
  ...
```

**Connection Behavior**

The server intentionally omits a `Content-Length` header, signaling an **unbounded response body**. The TCP connection remains open indefinitely, and the server continuously pushes audio bytes to the client in a real-time loop. The client reads from the socket as a **persistent data pipe**, decoding MP3/AAC frames as they arrive.

**Connection Termination**

The stream ends when any of the following occurs:
- The client closes the TCP socket (e.g., user stops playback)
- The server stops broadcasting (e.g., source encoder disconnects)
- A network interruption breaks the TCP connection
- The server delivers a finite amount of data (rare, for pre-recorded content)

Note: While the example above shows `HTTP/1.0`, modern SHOUTcast/Icecast servers may use `HTTP/1.1` with `Connection: keep-alive` and `Transfer-Encoding: chunked` to maintain the persistent stream.

**ICY Metadata Block Structure**

The metadata block is inserted into the byte stream at fixed intervals (`icy-metaint`). Its structure is:

```
Byte 0:       Length byte (n)
              Value = ceil(metadata_string_length / 16)
              A value of 0 means no metadata in this block

Bytes 1..n*16:  UTF-8 metadata string, padded with NULL bytes

Typical content:
  "StreamTitle='Artist - Song Title';StreamUrl='https://...';"
```

**Metadata Parsing Algorithm**

```javascript
function parseICYMetadata(block) {
  const lengthByte = block[0];
  if (lengthByte === 0) return null;

  const metadataLength = lengthByte * 16;
  const rawString = block.slice(1, metadataLength + 1)
    .toString('utf-8')
    .replace(/\x00/g, '');

  // Parse key='value'; pairs
  const result = {};
  const pairs = rawString.split(';');
  for (const pair of pairs) {
    const eqIndex = pair.indexOf('=');
    if (eqIndex > 0) {
      const key = pair.substring(0, eqIndex).trim();
      const value = pair.substring(eqIndex + 1).replace(/^'|'$/g, '');
      result[key] = value;
    }
  }
  return result;
}
```

**Byte-Level Stream Layout**

```
Offset (bytes)    Content
---------------------------------------------------
0..8191           MP3 audio frame(s)
8192              Metadata length byte (e.g., 0x03 = 48 bytes)
8193..8240        Metadata string (48 bytes, NULL-padded)
8241..16332       MP3 audio frame(s)
16333             Metadata length byte
16334..16381      Metadata string
...               Repeating pattern
```

**SHOUTcast Version 2 (SC2) Extensions**

SC2 adds UDP-based station directory queries and JSON-based metadata. The stream itself remains ICY-compatible but can carry additional ULM (Unified Metadata Language) frames:

```
ULM Frame Structure:
+------------------+------------------+------------------+
|  Sync Word (4)   |  Type (1)        |  Length (4)      |
|  0x554C4D00      |  0x01 = JSON     |  Network order   |
+------------------+------------------+------------------+
|  Payload (variable, JSON-encoded metadata)              |
+---------------------------------------------------------+
```


---

