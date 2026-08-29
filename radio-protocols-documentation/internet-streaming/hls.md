### HLS (HTTP Live Streaming)

**Overview**

HLS is Apple's adaptive bitrate streaming protocol. It segments a continuous media stream into short HTTP-downloadable chunks and describes them in a text-based playlist (M3U8). Originally video-focused, HLS is widely used for internet radio, especially by stations targeting iOS devices.

**Connection and Setup**

Player boot sequence:
```
Step 1: Fetch master playlist
  GET /master.m3u8 HTTP/1.1
  Host: cdn.example.com
  Accept: application/vnd.apple.mpegurl

  Response: #EXTM3U + variant list

Step 2: Select variant (bandwidth-based)
  Pick variant closest to measured throughput

Step 3: Fetch media playlist
  GET /high/playlist.m3u8 HTTP/1.1

  Response: #EXTM3U + segment list

Step 4: Fetch encryption key (if encrypted)
  #EXT-X-KEY:METHOD=AES-128,URI="https://.../key.bin"
  GET /key.bin HTTP/1.1
  Response: 16-byte AES key

Step 5: Download and play segments
  GET /high/segment_001.ts HTTP/1.1
  [decrypt with AES-128-CBC if needed, demux, decode]

Step 6: Live refresh loop
  Re-fetch media playlist every ~targetDuration/2
  Detect new segments by sequence number
```

**Architecture**

```
+------------------+     +------------------+     +------------------+
|   Audio Source   | --> |  Media Segmenter | --> |   Web Server     |
|  (Live encoder)  |     |  (ffmpeg / etc.) |     |  (CDN/Origin)    |
+------------------+     +------------------+     +------------------+
                               |                          |
                               v                          v
                         +------------------+     +------------------+
                         | Playlist (.m3u8) |     | Segments (.aac) |
                         | Updated live     |     | ~2-10 sec each  |
                         +------------------+     +------------------+
                                                          |
                               +--------------------------+
                               v
                    +-----------------------+
                    |      HLS Player       |
                    | Downloads playlist    |
                    | Fetches segments      |
                    | Concatenates & plays  |
                    +-----------------------+
```

**Master Playlist Structure**

The master playlist describes available variants (different bitrates/qualities):

```
#EXTM3U
#EXT-X-VERSION:4

# Variant 1: High quality
#EXT-X-STREAM-INF:BANDWIDTH=128000,CODECS="mp4a.40.2",AUDIO="aac"
https://cdn.example.com/high/playlist.m3u8

# Variant 2: Medium quality
#EXT-X-STREAM-INF:BANDWIDTH=64000,CODECS="mp4a.40.5",AUDIO="aac"
https://cdn.example.com/medium/playlist.m3u8

# Variant 3: Low quality
#EXT-X-STREAM-INF:BANDWIDTH=32000,CODECS="mp4a.40.5",AUDIO="aac"
https://cdn.example.com/low/playlist.m3u8
```

**Media Playlist Structure**

The media playlist lists individual segments:

```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:10       <-- Max segment duration
#EXT-X-MEDIA-SEQUENCE:12345    <-- Sequence number of first segment
#EXT-X-PLAYLIST-TYPE:LIVE      <-- LIVE or VOD

#EXTINF:9.984,                 <-- Duration in seconds
segment_12345.aac
#EXTINF:10.005,
segment_12346.aac
#EXTINF:9.978,
segment_12347.aac

#EXT-X-ENDLIST                 <-- Absent in live streams
```

**How HLS Segment Discovery Works**

The following points clarify the core behavior of the HLS protocol regarding segment availability, playlist polling, and connection model.

1. **The media playlist is the source of truth for available segments.**
   Fetching the media playlist returns a manifest that explicitly lists which media segments (downloadable chunks) are currently available. Each entry includes the segment URL and its duration. The player does not guess or predict what is available; it learns this solely from the playlist response.

2. **Live streams require continuous re-querying of the media playlist.**
   For live broadcasts, the server appends new segments to the media playlist over time and removes expired ones (in sliding-window streams). The client must repeatedly fetch the media playlist to discover newly available segments. The refresh interval is typically based on the `EXT-X-TARGETDURATION` tag (e.g., re-fetch every half target duration). Without this polling loop, the player will run out of buffered audio and stall.

3. **HLS does not require a persistent connection.**
   Each segment is downloaded via an independent, stateless HTTP `GET` request. Once a chunk is downloaded, the underlying TCP/HTTP connection may be closed. There is no need for a single long-lived connection carrying a continuous byte stream. This is fundamentally different from the ICY/Icecast protocol, which relies on a persistent HTTP connection over which the server pushes an unending stream of audio data. The HLS chunk-based model makes it compatible with standard HTTP infrastructure such as CDNs, proxies, and caches.

**Segment Container Formats**

| Profile | Container | Audio Codec | Typical Use |
|---------|-----------|-------------|-------------|
| AAC-LC | MPEG-2 TS or raw AAC | AAC-LC | Legacy compatibility |
| HE-AAC | MPEG-2 TS or raw AAC | HE-AAC v1/v2 | Low bitrate radio |
| AAC-FMP4 | Fragmented MP4 | AAC-LC/HE-AAC | Modern HLS (version 7+) |

**HLS Segment (MPEG-2 Transport Stream) Structure**

```
MPEG-2 TS Packet (188 bytes fixed):
+--------+--------+-------------------------------------------+
| Sync   | Flags  | Payload                                   |
| 0x47   | 3 bytes| 184 bytes                                 |
+--------+--------+-------------------------------------------+

Flags byte breakdown:
  Bits 0-12:  PID (Packet Identifier) - 0x0100 = video, 0x0101 = audio
  Bits 13-14: Scrambling control
  Bit 15:     Adaptation field present
  Bit 16:     Payload present

Adaptation Field (when present):
  +--------+--------+---------------------------------------+
  | Length | Flags  | Stuffing bytes / PCR timestamp        |
  | 1 byte | 1 byte | variable                              |
  +--------+--------+---------------------------------------+

PES Packet (Packetized Elementary Stream):
  +--------+--------+--------+--------------------------------+
  | Start  | Stream | Length | PTS/DTS timestamps + audio     |
  | Code   | ID     | 2 bytes| frames                         |
  | 0x000001| 1 byte|        |                                |
  +--------+--------+--------+--------------------------------+
```

**ID3 Metadata in HLS**

Timed metadata is carried in ID3 tags within segments, typically in a separate "timed metadata" PID or at segment boundaries:

```
ID3v2.4 Tag Structure:
+------------------+------------------+------------------+
| "ID3" (3 bytes)  | Version (2)      | Flags (1)        |
|                  | 0x04 0x00        | %abcd0000        |
+------------------+------------------+------------------+
| Tag Size (4 bytes, synchsafe integer)                   |
+---------------------------------------------------------+
| Frames (variable):                                      |
|   TIT2 - Title                                          |
|   TPE1 - Lead performer(s)                              |
|   TALB - Album                                          |
|   TXXX - User-defined text (e.g., "TXXX:RadioStation")  |
+---------------------------------------------------------+
```

**Parsing an HLS Live Stream**

```javascript
async function playHLS(masterPlaylistUrl) {
  // Step 1: Fetch master playlist
  const master = await fetch(masterPlaylistUrl).then(r => r.text());
  const variants = parseVariants(master);

  // Step 2: Select appropriate variant based on bandwidth
  const selected = selectVariant(variants, currentBandwidth);

  // Step 3: Fetch media playlist
  let mediaPlaylist = await fetch(selected.url).then(r => r.text());
  let segments = parseSegments(mediaPlaylist);

  // Step 4: Start download loop
  while (streamIsActive) {
    for (const segment of segments) {
      if (!bufferHas(segment.url)) {
        const data = await fetch(segment.url).then(r => r.arrayBuffer());
        demux(data);        // Extract audio from TS/FMP4
        decode(data);       // Decode AAC/HE-AAC
        enqueueToPlaybackQueue(data);
      }
    }

    // Step 5: Refresh playlist for live streams
    if (isLive) {
      await sleep(targetDuration / 2);
      mediaPlaylist = await fetch(selected.url).then(r => r.text());
      const newSegments = parseSegments(mediaPlaylist);
      segments = mergeNewSegments(segments, newSegments);
    }
  }
}
```


---

