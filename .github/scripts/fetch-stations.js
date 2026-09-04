/**
 * Fetch all radio stations from Radio Browser API and save to radio-stations/.
 * Writes:
 *   - radio-stations/stations.json          (all stations, backward compatible)
 *   - radio-stations/index.json             (manifest)
 *   - radio-stations/{page}/stations.json   (paginated shards, 10k per page)
 * This script is intended to be run by GitHub Actions.
 */

const fs = require('fs');
const path = require('path');

// Radio Browser API server pool for redundancy
const RADIO_BROWSER_SERVERS = [
  'https://de1.api.radio-browser.info',
  'https://nl1.api.radio-browser.info',
  'https://fr1.api.radio-browser.info',
  'https://at1.api.radio-browser.info',
  'https://us1.api.radio-browser.info'
];

const API_TIMEOUT_MS = 8000;
const API_USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36';
const PAGE_SIZE = 10000;

async function fetchFromRadioBrowser(apiPath, options = {}) {
  const timeoutMs = options.timeout || API_TIMEOUT_MS;
  const errors = [];

  for (const baseUrl of RADIO_BROWSER_SERVERS) {
    const url = `${baseUrl}${apiPath}`;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const fetchOptions = {
        signal: controller.signal,
        headers: {
          'User-Agent': API_USER_AGENT,
          ...options.headers
        }
      };

      const response = await fetch(url, fetchOptions);
      clearTimeout(timeoutId);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      clearTimeout(timeoutId);
      const msg = error.name === 'AbortError' ? 'Request timed out' : (error.message || error.name);
      errors.push(`${baseUrl}: ${msg}`);
      console.warn(`Radio Browser server failed: ${baseUrl} - ${msg}`);
    }
  }

  throw new Error(`All Radio Browser servers failed. Errors: ${errors.join('; ')}`);
}

function transformStation(station) {
  return {
    stationuuid: station.stationuuid,
    name: station.name,
    url_resolved: station.url_resolved || station.url,
    favicon: station.favicon,
    tags: station.tags ? station.tags.split(',').map(t => t.trim()).filter(Boolean) : [],
    country: station.country,
    countrycode: station.countrycode,
    bitrate: station.bitrate,
    codec: station.codec,
    votes: station.votes,
    language: station.language || '',
    languagecodes: station.languagecodes || '',
    hls: station.hls || 0,
    geo_lat: station.geo_lat ?? null,
    geo_long: station.geo_long ?? null
  };
}

function cleanupRadioStationsDir(outputDir) {
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
    return;
  }

  const entries = fs.readdirSync(outputDir, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.name === 'index.html') continue;

    const fullPath = path.join(outputDir, entry.name);
    if (entry.isDirectory()) {
      fs.rmSync(fullPath, { recursive: true, force: true });
    } else {
      fs.unlinkSync(fullPath);
    }
  }
}

async function fetchAllStations(outputBaseDir) {
  console.log('Fetching all stations from Radio Browser...');
  const startTime = Date.now();
  let offset = 0;
  let allStations = [];
  let pageCount = 0;
  let totalPages = 0;

  while (true) {
    const apiPath = `/json/stations/search?limit=${PAGE_SIZE}&offset=${offset}&order=votes&reverse=true&hidebroken=true&lastcheckok=1`;

    let page = null;
    let lastError = null;
    for (let attempt = 1; attempt <= 3; attempt++) {
      try {
        page = await fetchFromRadioBrowser(apiPath);
        break;
      } catch (error) {
        lastError = error;
        console.warn(`Page ${pageCount} attempt ${attempt} failed: ${error.message}`);
        if (attempt < 3) {
          await new Promise(resolve => setTimeout(resolve, 1000));
        }
      }
    }

    if (page === null) {
      throw new Error(`Page ${pageCount} failed after 3 attempts: ${lastError.message}`);
    }

    if (page.length === 0) break;

    const transformedPage = page.map(transformStation);

    // Write per-page shard
    const pageDir = path.join(outputBaseDir, String(pageCount));
    fs.mkdirSync(pageDir, { recursive: true });
    const pagePath = path.join(pageDir, 'stations.json');
    fs.writeFileSync(pagePath, JSON.stringify(transformedPage, null, 0));

    allStations = allStations.concat(transformedPage);
    totalPages++;
    console.log(`  Page ${pageCount}: +${transformedPage.length} stations (total: ${allStations.length})`);

    if (page.length < PAGE_SIZE) break;
    pageCount++;
    offset += PAGE_SIZE;
  }

  const duration = Date.now() - startTime;
  console.log(`Fetched ${allStations.length} stations in ${duration}ms across ${totalPages} page(s)`);

  return { stations: allStations, totalPages };
}

function writeManifest(outputDir, totalStations, totalPages) {
  const pages = Array.from({ length: totalPages }, (_, i) => {
    const expectedCount = i === totalPages - 1
      ? totalStations - (i * PAGE_SIZE)
      : PAGE_SIZE;
    return {
      page: i,
      path: `/${i}/stations.json`,
      count: expectedCount
    };
  });

  const manifest = {
    totalStations,
    pageSize: PAGE_SIZE,
    totalPages,
    lastUpdated: new Date().toISOString(),
    pages
  };

  const manifestPath = path.join(outputDir, 'index.json');
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
  console.log(`Wrote manifest to ${manifestPath}`);
}

async function main() {
  try {
    const outputDir = path.resolve(__dirname, '..', '..', 'radio-stations');

    cleanupRadioStationsDir(outputDir);
    console.log(`Cleaned up ${outputDir} (preserved index.html)`);

    const { stations, totalPages } = await fetchAllStations(outputDir);

    // Write consolidated stations.json (backward compatible)
    const stationsPath = path.join(outputDir, 'stations.json');
    fs.writeFileSync(stationsPath, JSON.stringify(stations, null, 0));

    const stats = fs.statSync(stationsPath);
    console.log(`Wrote ${stations.length} stations to ${stationsPath} (${(stats.size / 1024 / 1024).toFixed(2)} MB)`);

    // Write manifest
    writeManifest(outputDir, stations.length, totalPages);
  } catch (error) {
    console.error('Failed to fetch stations:', error);
    process.exit(1);
  }
}

main();
