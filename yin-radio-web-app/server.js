const express = require('express');
const { pipeline } = require('stream');
const http = require('http');
const https = require('https');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Static files for the web app
app.use(express.static('public'));

// Serve radio-stations data folder from repo root
app.use('/radio-stations', express.static(path.join(__dirname, '..', 'radio-stations')));

// GET /proxy - Proxy audio streams to bypass CORS
app.get('/proxy', async (req, res) => {
  const streamUrl = req.query.url;

  if (!streamUrl) {
    return res.status(400).json({ error: 'Missing url parameter' });
  }

  let parsedUrl;
  try {
    parsedUrl = new URL(streamUrl);
  } catch (err) {
    return res.status(400).json({ error: 'Invalid URL' });
  }

  const protocol = parsedUrl.protocol === 'https:' ? https : http;
  const requestOptions = {
    hostname: parsedUrl.hostname,
    port: parsedUrl.port || (parsedUrl.protocol === 'https:' ? 443 : 80),
    path: parsedUrl.pathname + parsedUrl.search,
    method: 'GET',
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    },
    // Force IPv4 to avoid ETIMEDOUT when IPv6 is unreachable but DNS returns AAAA records
    family: 4,
    // Allow legacy/renegotiation for some older Icecast servers
    minVersion: 'TLSv1'
  };

  const upstreamReq = protocol.request(requestOptions, (upstreamRes) => {
    // Forward status code (but only if it's not a redirect we want to handle)
    // For audio streams, pass through whatever we got
    const statusCode = upstreamRes.statusCode;

    if (statusCode >= 400) {
      console.error(`Upstream returned error status: ${statusCode} for ${streamUrl}`);
      if (!res.headersSent) {
        return res.status(statusCode).json({ error: `Upstream error: ${statusCode}` });
      }
      return;
    }

    // Forward headers
    const contentType = upstreamRes.headers['content-type'];
    if (contentType) {
      res.setHeader('Content-Type', contentType);
    }

    const contentLength = upstreamRes.headers['content-length'];
    if (contentLength) {
      res.setHeader('Content-Length', contentLength);
    }

    // Prevent browser from sending Range resume requests on live streams
    res.setHeader('Accept-Ranges', 'none');

    // Forward all ICY / Icecast headers
    Object.keys(upstreamRes.headers).forEach((key) => {
      const lowerKey = key.toLowerCase();
      if (lowerKey.startsWith('icy-') || lowerKey.startsWith('ice-')) {
        res.setHeader(key, upstreamRes.headers[key]);
      }
    });

    // Pipe the stream
    if (upstreamRes) {
      pipeline(upstreamRes, res, (err) => {
        if (err) {
          console.error('Stream pipeline error:', err.message, '| Code:', err.code);
        }
      });
    } else {
      res.end();
    }
  });

  upstreamReq.on('error', (error) => {
    console.error('Proxy error:', error.message, '| Code:', error.code);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Failed to proxy stream' });
    } else {
      res.end();
    }
  });

  // Handle client disconnect
  res.on('close', () => {
    if (!res.writableEnded) {
      upstreamReq.destroy();
    }
  });

  upstreamReq.end();
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.listen(PORT, () => {
  console.log(`Yin Radio app running on http://localhost:${PORT}`);
});
