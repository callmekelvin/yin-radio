function radioApp() {
  return {
    // State
    allStations: [],
    stations: [],
    countries: [],
    tags: [],
    languages: [],
    currentStation: null,
    isPlaying: false,
    isLoading: false,
    isBuffering: false,
    volume: 0.8,
    errorMessage: null,
    selectedStationInfo: null,

    // Data loading state
    dataLoaded: false,
    dataLoading: false,

    // Pagination state
    offset: 0,
    limit: 30,
    hasMore: true,
    isLoadingMore: false,

    // Filters
    searchQuery: '',
    selectedCountry: '',
    selectedLanguage: '',
    selectedTag: '',

    // Audio element
    audio: null,

    // Reconnect state
    reconnectAttempts: 0,
    reconnectTimer: null,
    stalledTimer: null,

    async init() {
      // Initialize audio element
      this.audio = new Audio();
      this.audio.volume = this.volume;

      // Set up audio event listeners
      this.audio.addEventListener('play', () => {
        this.isPlaying = true;
        this.isBuffering = false;
        this.clearReconnect();
      });

      this.audio.addEventListener('pause', () => {
        this.isPlaying = false;
      });

      this.audio.addEventListener('waiting', () => {
        this.isBuffering = true;
        this.clearStalledTimer();
      });

      this.audio.addEventListener('canplay', () => {
        this.isBuffering = false;
        this.clearStalledTimer();
      });

      this.audio.addEventListener('error', (e) => {
        console.error('Audio error:', e);
        this.isPlaying = false;
        this.isBuffering = false;
        if (this.currentStation) {
          this.scheduleReconnect();
        }
      });

      this.audio.addEventListener('stalled', () => {
        this.isBuffering = true;
        this.clearStalledTimer();
        this.stalledTimer = setTimeout(() => {
          if (this.isPlaying && this.currentStation) {
            console.warn('Stalled for 5s, triggering reconnect');
            this.scheduleReconnect();
          }
        }, 5000);
      });

      // Load stations JSON
      await this.loadStations();

      // Set up infinite scroll listener
      this.setupScrollListener();
    },

    setupScrollListener() {
      let ticking = false;
      window.addEventListener('scroll', () => {
        if (!ticking) {
          requestAnimationFrame(() => {
            this.checkScroll();
            ticking = false;
          });
          ticking = true;
        }
      });
    },

    checkScroll() {
      if (!this.dataLoaded || this.isLoadingMore || !this.hasMore) return;

      const scrollBottom = window.innerHeight + window.scrollY;
      const docHeight = document.documentElement.scrollHeight;

      if (scrollBottom >= docHeight - 500) {
        this.loadMore();
      }
    },

    async loadStations() {
      this.dataLoading = true;
      this.errorMessage = null;

      try {
        const response = await fetch('/radio-stations/stations.json');
        if (!response.ok) {
          throw new Error('HTTP error: ' + response.status);
        }

        this.allStations = await response.json();
        this.dataLoaded = true;

        // Compute initial filter counts and paginated stations
        this.fetchFilterCounts();
        this.applyClientPagination(true);
      } catch (error) {
        console.error('Error loading stations:', error);
        this.errorMessage = 'Failed to load station data. Please try again.';
      } finally {
        this.dataLoading = false;
      }
    },

    clearError() {
      this.errorMessage = null;
      this.clearReconnect();
    },

    buildFilterParams() {
      return {
        search: this.searchQuery.trim(),
        country: this.selectedCountry,
        tag: this.selectedTag,
        language: this.selectedLanguage
      };
    },

    applyFilters(stations, { search, country, tag, language }) {
      let results = stations;

      if (search) {
        const query = search.toLowerCase();
        results = results.filter(s => s.name.toLowerCase().includes(query));
      }
      if (country) {
        results = results.filter(s => s.countrycode === country);
      }
      if (tag) {
        const tagQuery = tag.toLowerCase();
        results = results.filter(s => s.tags.some(t => t.toLowerCase() === tagQuery));
      }
      if (language) {
        const langQuery = language.toLowerCase();
        results = results.filter(s =>
          s.language.toLowerCase().includes(langQuery) ||
          s.languagecodes.toLowerCase().includes(langQuery)
        );
      }

      return results;
    },

    deriveCountries(stations) {
      const countryMap = new Map();
      stations.forEach(s => {
        if (s.countrycode) {
          const existing = countryMap.get(s.countrycode);
          if (existing) {
            existing.stationcount++;
          } else {
            countryMap.set(s.countrycode, {
              name: s.country || s.countrycode,
              code: s.countrycode,
              stationcount: 1
            });
          }
        }
      });
      return Array.from(countryMap.values())
        .sort((a, b) => b.stationcount - a.stationcount);
    },

    deriveTags(stations) {
      const tagMap = new Map();
      stations.forEach(s => {
        s.tags.forEach(tag => {
          if (tag) {
            const key = tag.toLowerCase();
            const existing = tagMap.get(key);
            if (existing) {
              existing.stationcount++;
            } else {
              tagMap.set(key, { name: tag, stationcount: 1 });
            }
          }
        });
      });
      return Array.from(tagMap.values())
        .sort((a, b) => b.stationcount - a.stationcount);
    },

    deriveLanguages(stations) {
      const langMap = new Map();
      stations.forEach(s => {
        if (s.language) {
          s.language.split(',').map(l => l.trim()).filter(Boolean).forEach(lang => {
            const existing = langMap.get(lang);
            if (existing) {
              existing.stationcount++;
            } else {
              langMap.set(lang, { name: lang, stationcount: 1 });
            }
          });
        }
      });
      return Array.from(langMap.values())
        .sort((a, b) => b.stationcount - a.stationcount);
    },

    fetchFilterCounts() {
      const filters = this.buildFilterParams();

      // For countries: count stations matching search+tag+language (excluding country)
      const countryFiltered = this.applyFilters(this.allStations, {
        search: filters.search,
        tag: filters.tag,
        language: filters.language
      });
      this.countries = this.preserveSelected(
        this.deriveCountries(countryFiltered),
        this.selectedCountry,
        'code'
      );

      // For tags: count stations matching search+country+language (excluding tag)
      const tagFiltered = this.applyFilters(this.allStations, {
        search: filters.search,
        country: filters.country,
        language: filters.language
      });
      this.tags = this.preserveSelected(
        this.deriveTags(tagFiltered),
        this.selectedTag,
        'name'
      );

      // For languages: count stations matching search+country+tag (excluding language)
      const langFiltered = this.applyFilters(this.allStations, {
        search: filters.search,
        country: filters.country,
        tag: filters.tag
      });
      this.languages = this.preserveSelected(
        this.deriveLanguages(langFiltered),
        this.selectedLanguage,
        'name'
      );
    },

    preserveSelected(items, selectedValue, keyField) {
      if (!selectedValue) return items;
      const found = items.some(item => item[keyField] === selectedValue);
      if (!found) {
        const preserved = { ...items[0] };
        preserved[keyField] = selectedValue;
        preserved.stationcount = 0;
        if (keyField === 'name') preserved.name = selectedValue;
        if (keyField === 'code') {
          preserved.code = selectedValue;
          preserved.name = selectedValue;
        }
        return [preserved, ...items];
      }
      return items;
    },

    applyClientPagination(reset = false) {
      if (reset) {
        this.offset = 0;
        this.hasMore = true;
        this.stations = [];
        this.isLoading = true;
      } else {
        this.isLoadingMore = true;
      }

      const filters = this.buildFilterParams();
      const filtered = this.applyFilters(this.allStations, filters);

      const start = this.offset;
      const end = start + this.limit;
      const page = filtered.slice(start, end);

      if (reset) {
        this.stations = page;
      } else {
        this.stations = this.stations.concat(page);
      }

      this.hasMore = page.length === this.limit && this.stations.length < filtered.length;
      this.isLoading = false;
      this.isLoadingMore = false;
    },

    fetchStations(reset = false) {
      this.applyClientPagination(reset);
    },

    loadMore() {
      if (!this.hasMore || this.isLoadingMore) return;
      this.offset += this.limit;
      this.applyClientPagination(false);
    },

    async playStation(station) {
      // If clicking the same station, toggle play/pause
      if (this.currentStation && this.currentStation.stationuuid === station.stationuuid) {
        this.togglePlay();
        return;
      }

      // Clear any pending reconnect state before starting a new stream
      this.clearReconnect();

      // Stop current playback
      this.audio.pause();
      this.audio.currentTime = 0;

      // Set new source through proxy
      const streamUrl = station.url_resolved || station.url;
      this.audio.src = '/proxy?url=' + encodeURIComponent(streamUrl);
      this.audio.volume = this.volume;
      this.currentStation = station;
      this.isBuffering = true;

      // Start playback
      const playPromise = this.audio.play();
      if (playPromise) {
        playPromise.catch(err => {
          console.error('Play error:', err);
          this.isPlaying = false;
          this.isBuffering = false;
        });
      }
    },

    togglePlay() {
      if (!this.currentStation) return;

      if (this.audio.paused) {
        const playPromise = this.audio.play();
        if (playPromise) {
          playPromise.catch(err => {
            console.error('Play error:', err);
          });
        }
      } else {
        this.audio.pause();
        this.clearReconnect();
      }
    },

    clearStalledTimer() {
      if (this.stalledTimer) {
        clearTimeout(this.stalledTimer);
        this.stalledTimer = null;
      }
    },

    clearReconnect() {
      this.clearStalledTimer();
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
      this.reconnectAttempts = 0;
    },

    scheduleReconnect() {
      if (this.reconnectAttempts >= 5) {
        this.errorMessage = 'Stream unavailable. Please try another station.';
        this.isBuffering = false;
        return;
      }

      this.reconnectAttempts++;
      this.isBuffering = true;
      const delay = Math.min(2 * this.reconnectAttempts, 30) * 1000;

      console.log(`Scheduling reconnect attempt ${this.reconnectAttempts}/5 in ${delay}ms`);

      this.reconnectTimer = setTimeout(() => {
        if (this.currentStation) {
          this.playStation(this.currentStation);
        }
      }, delay);
    },

    setVolume(value) {
      this.volume = parseFloat(value);
      if (this.audio) {
        this.audio.volume = this.volume;
      }
    },

    clearFilters() {
      this.searchQuery = '';
      this.selectedCountry = '';
      this.selectedLanguage = '';
      this.selectedTag = '';
      this.fetchFilterCounts();
      this.fetchStations(true);
    },

    showStationInfo(station) {
      this.selectedStationInfo = station;
    },

    closeStationInfo() {
      this.selectedStationInfo = null;
    }
  };
}
