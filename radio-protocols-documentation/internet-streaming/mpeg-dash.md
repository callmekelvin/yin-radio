### MPEG-DASH

**Overview**

MPEG-DASH (Dynamic Adaptive Streaming over HTTP) is the ISO/IEC 23009-1 standard for adaptive streaming. Unlike HLS, which is Apple-proprietary, DASH is an open international standard. It uses an XML-based Media Presentation Description (MPD) manifest to describe segmented content.

**Connection and Setup**

Player boot sequence:
```
Step 1: Fetch MPD manifest
  GET /manifest.mpd HTTP/1.1
  Host: cdn.example.com
  Accept: application/dash+xml

  Response: XML MPD document

Step 2: Parse MPD
  Extract Periods, AdaptationSets, Representations
  Read SegmentTemplate, SegmentTimeline, BaseURL

Step 3: Select representation
  Filter by codec capability
  Select by bandwidth (throughput * 0.8)

Step 4: Fetch initialization segment
  GET /audio_128k/init.mp4 HTTP/1.1
  Response: moov box (track headers)

Step 5: Download media segments
  GET /audio_128k/0.m4s HTTP/1.1
  [append to SourceBuffer or decode]

Step 6: Live refresh (dynamic MPD)
  If MPD@type="dynamic":
    Re-fetch MPD every minimumUpdatePeriod seconds
    Merge updated SegmentTimeline
```

**How DASH Segment Discovery Works**

The following points clarify the core behavior of the MPEG-DASH protocol regarding segment availability, manifest polling, and connection model.

1. **The MPD manifest is the source of truth for available segments.**
   Fetching the MPD returns an XML document that describes the media presentation, including which segments are available for download. Segments are declared explicitly via `SegmentTimeline`, `SegmentList`, or inferred from `SegmentTemplate` rules (e.g., `$Number$` or `$Time$` addressing). The player learns what is available solely from the manifest response.

2. **Live streams require continuous re-querying of the MPD manifest.**
   For live broadcasts, the MPD is dynamic (`type="dynamic"`). The server updates the manifest over time by appending new segments to `SegmentTimeline` (or advancing the template window). The client must repeatedly fetch the MPD to discover newly available segments, typically at the interval specified by the `minimumUpdatePeriod` attribute (e.g., every 2 seconds). Without this polling loop, the player will run out of buffered audio and stall.

3. **DASH does not require a persistent connection.**
   Each segment—including the initialization segment (`init.mp4`) and each media fragment (`.m4s`)—is downloaded via an independent, stateless HTTP `GET` request. Once a chunk is downloaded, the underlying TCP/HTTP connection may be closed. There is no need for a single long-lived connection carrying a continuous byte stream. This is fundamentally different from the ICY/Icecast protocol, which relies on a persistent HTTP connection over which the server pushes an unending stream of audio data. The DASH chunk-based model makes it compatible with standard HTTP infrastructure such as CDNs, proxies, and caches.

**Architecture Comparison: HLS vs DASH**

```
HLS:                              DASH:
+------------+                    +------------+
| Master     |                    | MPD        |
| Playlist   |                    | Manifest   |
| (.m3u8)    |                    | (.mpd)     |
+-----+------+                    +-----+------+
      |                                 |
      v                                 v
+------------+                    +------------+
| Media      |                    | Period ->  |
| Playlist   |                    | Adaptation ->
| (.m3u8)    |                    | Representation|
+-----+------+                    +------------+
      |                                 |
      v                                 v
+------------+                    +------------+
| .ts/.aac   |                    | .m4s (FMP4)|
| Segments   |                    | Segments   |
+------------+                    +------------+
```

**MPD Manifest Structure**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<MPD xmlns="urn:mpeg:dash:schema:mpd:2011"
     type="dynamic"
     availabilityStartTime="2026-08-28T12:00:00Z"
     minimumUpdatePeriod="PT2S"
     minBufferTime="PT4S"
     profiles="urn:mpeg:dash:profile:isoff-live:2011">

  <Period id="p0" start="PT0S">

    <!-- Audio Adaptation Set -->
    <AdaptationSet id="0"
                   contentType="audio"
                   lang="en"
                   segmentAlignment="true">

      <!-- Representation: High Quality -->
      <Representation id="audio_128k"
                      mimeType="audio/mp4"
                      codecs="mp4a.40.2"
                      audioSamplingRate="48000"
                      bandwidth="128000">
        <AudioChannelConfiguration
          schemeIdUri="urn:mpeg:dash:23003:3:audio_channel_configuration:2011"
          value="2"/>
        <SegmentTemplate
          timescale="48000"
          initialization="audio_128k/init.mp4"
          media="audio_128k/$Time$.m4s">
          <SegmentTimeline>
            <S t="0" d="192000" r="29"/>    <!-- 4-second segments -->
          </SegmentTimeline>
        </SegmentTemplate>
      </Representation>

      <!-- Representation: Low Quality -->
      <Representation id="audio_64k"
                      mimeType="audio/mp4"
                      codecs="mp4a.40.5"
                      audioSamplingRate="44100"
                      bandwidth="64000">
        <AudioChannelConfiguration
          schemeIdUri="urn:mpeg:dash:23003:3:audio_channel_configuration:2011"
          value="2"/>
        <SegmentTemplate
          timescale="44100"
          initialization="audio_64k/init.mp4"
          media="audio_64k/$Time$.m4s">
          <SegmentTimeline>
            <S t="0" d="176400" r="29"/>
          </SegmentTimeline>
        </SegmentTemplate>
      </Representation>

    </AdaptationSet>

  </Period>

</MPD>
```

**DASH Segment Addressing Modes**

| Mode | Description | URL Pattern |
|------|-------------|-------------|
| **SegmentTemplate + $Number$** | Sequential numbering | `segment_$Number$.m4s` |
| **SegmentTemplate + $Time$** | Time-based addressing | `segment_$Time$.m4s` |
| **SegmentList** | Explicit URL list | `<SegmentURL media="seg1.m4s"/>` |
| **SegmentBase** | Single file with byte ranges | `Range: bytes=0-99999` |

**Fragmented MP4 (FMP4) Segment Structure**

```
FMP4 File Structure:
+------------------+------------------+------------------+
| ftyp (File Type) | moov (Movie Header)| moof (Movie    |
|                  |   + trak + mvex  |   Fragment Header)|
+------------------+------------------+------------------+
| mdat (Media Data - encrypted/compressed audio frames)  |
+---------------------------------------------------------+
| moof (Next fragment header)                             |
+---------------------------------------------------------+
| mdat (Next fragment data)                               |
+---------------------------------------------------------+
| ...                                                     |

Box Format (all boxes):
+------------------+------------------+------------------+
| Size (4 bytes)   | Type (4 bytes)   | Data (size-8)    |
| Network order    | ASCII (e.g.,     |                  |
|                  | "moof", "mdat")  |                  |
+------------------+------------------+------------------+

moof -> traf -> tfhd (track fragment header)
            -> trun (track run - sample offsets/sizes)
            -> tfdt (track fragment decode time)
```

**Bandwidth Adaptation Logic**

```javascript
function selectRepresentation(adaptationSet) {
  // Measure recent throughput
  const throughput = measureDownloadSpeed(lastNSegments);

  // Filter representations by codec support
  const supported = adaptationSet.representations.filter(r =>
    canDecode(r.codecs)
  );

  // Select highest bitrate under throughput * safetyMargin
  const safetyMargin = 0.8;
  const targetBitrate = throughput * safetyMargin;

  let selected = supported
    .filter(r => r.bandwidth <= targetBitrate)
    .sort((a, b) => b.bandwidth - a.bandwidth)[0];

  // Fallback to lowest if none fit
  if (!selected) {
    selected = supported.sort((a, b) => a.bandwidth - b.bandwidth)[0];
  }

  return selected;
}
```


---

