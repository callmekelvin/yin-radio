### Icecast

**Overview**

Icecast is an open-source streaming server that implements and extends the SHOUTcast protocol. It supports multiple mount points, Ogg container formats (Vorbis, Opus, Theora), and WebM streaming. Icecast 2.4+ adds CORS headers and JSON status interfaces.

> **Related:** For the base protocol that Icecast extends, see [ICY / SHOUTcast](./icy-shoutcast.md).

**Protocol Differences from SHOUTcast**

| Feature | SHOUTcast | Icecast |
|---------|-----------|---------|
| Container formats | MP3, AAC | MP3, AAC, Ogg, Opus, WebM, FLAC |
| Multiple streams per server | No (separate ports) | Yes (mount points: `/stream.ogg`, `/stream.mp3`) |
| Standard HTTP headers | Partial | Full HTTP/1.1 compliance |
| CORS support | No | Yes (`Access-Control-Allow-Origin: *`) |
| Metadata protocol | ICY inline | ICY inline + Ogg metadata pages |
| Admin interface | HTTP GET params | XSLT/XML-based status + HTTP admin API |

**Connection and Setup**

Icecast uses standard HTTP/1.1 for both playback and source ingestion.

Playback handshake (client listens):
```
Client -> Server:
  GET /stream.mp3 HTTP/1.1
  Host: radio.example.com
  Accept: */*
  User-Agent: Mozilla/5.0
  Icy-MetaData: 1

Server -> Client:
  HTTP/1.1 200 OK
  Content-Type: audio/mpeg
  icy-name: Station Name
  icy-metaint: 8192
  Access-Control-Allow-Origin: *
  [audio stream follows]
```

Source handshake (broadcaster sends):
```
Client -> Server:
  SOURCE /stream.mp3 HTTP/1.1
  Host: radio.example.com
  Authorization: Basic base64(user:password)
  Content-Type: audio/mpeg
  Ice-Public: 1
  Ice-Name: My Station
  Ice-Genre: Rock
  Ice-Bitrate: 128
  [audio stream follows]

Server -> Client:
  HTTP/1.0 200 OK
```

**Ogg Container Metadata (Vorbis Comment)**

For Ogg-based streams (Vorbis, Opus, FLAC), metadata is not inline but embedded in Ogg pages:

```
Ogg Page Structure:
+------------------+------------------+------------------+
| Capture Pattern  | Stream Structure | Header Type      |
| "OggS" (4 bytes) | Version (1)      | Flags (1)        |
+------------------+------------------+------------------+
| Granule Position (8 bytes)                             |
+---------------------------------------------------------+
| Bitstream Serial Number (4)  | Page Sequence Number (4)|
+------------------------------+-------------------------+
| CRC Checksum (4)             | Number Page Segments (1)|
+------------------------------+-------------------------+
| Segment Table (variable)     | Segment Data            |
+---------------------------------------------------------+
```

**Vorbis Comment Header (inside Ogg page)**

```
Field           Size          Description
---------------------------------------------------
Packet Type     1 byte        0x03 = Vorbis Comment
Magic           6 bytes       "vorbis"
Vendor Length   4 bytes LE    Length of vendor string
Vendor String   variable      UTF-8 encoded
User Comments   4 bytes LE    Number of comment fields

For each comment:
  Length        4 bytes LE    Length of comment string
  Comment       variable      "FIELD=value" (e.g., "ARTIST=John Coltrane")

Framing Bit     1 byte        0x01 (required by spec)
```

**Icecast Status Endpoint**

Icecast exposes an XML status page at `/status.xsl` or raw XML at `/status-json.xsl`:

```xml
<icestats>
  <admin>admin@example.com</admin>
  <host>radio.example.com</host>
  <source mount="/stream.mp3">
    <listeners>42</listeners>
    <listener_peak>128</listener_peak>
    <audio_info>
      <bitrate>128</bitrate>
      <channels>2</channels>
      <samplerate>44100</samplerate>
    </audio_info>
    <genre>Jazz</genre>
    <server_description>Smooth Jazz 24/7</server_description>
    <server_name>Smooth Jazz Radio</server_name>
    <server_type>audio/mpeg</server_type>
    <stream_start>28/Aug/2026:12:00:00</stream_start>
    <title>John Coltrane - A Love Supreme</title>
  </source>
</icestats>
```


---

