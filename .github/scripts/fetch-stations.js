/**
 * Fetch all radio stations from Radio Browser API and save to public/stations.json
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

async function fetchFromRadioBrowser(path, options = {}) {
  const timeoutMs = options.timeout || API_TIMEOUT_MS;
  const errors = [];

  for (const baseUrl of RADIO_BROWSER_SERVERS) {
    const url = `${baseUrl}${path}`;
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

async function fetchAllStations() {
  console.log('Fetching all stations from Radio Browser...');
  const startTime = Date.now();
  const PAGE_SIZE = 10000;
  let offset = 0;
  let allStations = [];
  let pageCount = 0;

  while (true) {
    const path = `/json/stations/search?limit=${PAGE_SIZE}&offset=${offset}&order=votes&reverse=true&hidebroken=true&lastcheckok=1`;

    let page = null;
    let lastError = null;
    for (let attempt = 1; attempt <= 3; attempt++) {
      try {
        page = await fetchFromRadioBrowser(path);
        break;
      } catch (error) {
        lastError = error;
        console.warn(`Page ${pageCount + 1} attempt ${attempt} failed: ${error.message}`);
        if (attempt < 3) {
          await new Promise(resolve => setTimeout(resolve, 1000));
        }
      }
    }

    if (page === null) {
      throw new Error(`Page ${pageCount + 1} failed after 3 attempts: ${lastError.message}`);
    }

    if (page.length === 0) break;

    allStations = allStations.concat(page);
    pageCount++;
    console.log(`  Page ${pageCount}: +${page.length} stations (total: ${allStations.length})`);

    if (page.length < PAGE_SIZE) break;
    offset += PAGE_SIZE;
  }

  const transformed = allStations.map(transformStation);
  const duration = Date.now() - startTime;
  console.log(`Fetched ${transformed.length} stations in ${duration}ms`);

  return transformed;
}

async function main() {
  try {
    const stations = await fetchAllStations();

    const outputPath = path.resolve(__dirname, '..', '..', 'radio-stations', 'stations.json');
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });
    fs.writeFileSync(outputPath, JSON.stringify(stations, null, 0));

    const stats = fs.statSync(outputPath);
    console.log(`Wrote ${stations.length} stations to ${outputPath} (${(stats.size / 1024 / 1024).toFixed(2)} MB)`);
  } catch (error) {
    console.error('Failed to fetch stations:', error);
    process.exit(1);
  }
}

main();
