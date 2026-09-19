package com.yin_radio.yin_radio_android_app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yin_radio.yin_radio_android_app.data.local.db.TagEntity
import com.yin_radio.yin_radio_android_app.data.repository.StationRepository
import com.yin_radio.yin_radio_android_app.domain.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val stationRepository: StationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        Log.v(TAG, "Initialized")
        loadFilterOptions()
        submitSearch()
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                val countries = stationRepository.getDistinctCountries()
                val languages = stationRepository.getDistinctLanguageCodes()
                _uiState.update { it.copy(availableCountries = countries, availableLanguages = languages) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load filter options", e)
            }
        }
        viewModelScope.launch {
            try {
                stationRepository.getAllTags().collect { tags ->
                    _uiState.update { it.copy(availableTags = tags) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load tags", e)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        Log.v(TAG, "onSearchQueryChange() called")
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCountrySelected(country: String) {
        Log.v(TAG, "onCountrySelected() called")
        _uiState.update { it.copy(selectedCountry = country) }
    }

    fun onLanguageSelected(language: String) {
        Log.v(TAG, "onLanguageSelected() called")
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun onTagsSelected(tags: List<String>) {
        Log.v(TAG, "onTagsSelected() called")
        _uiState.update { it.copy(selectedTags = tags) }
    }

    fun submitSearch() {
        Log.v(TAG, "submitSearch() called")
        performSearch()
    }

    fun clearFilters() {
        Log.v(TAG, "clearFilters() called")
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedCountry = "",
                selectedLanguage = "",
                selectedTags = emptyList(),
                offset = 0
            )
        }
        submitSearch()
    }

    fun loadMore() {
        Log.v(TAG, "loadMore() called")
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        _uiState.update { it.copy(offset = it.offset + 30) }
        performSearch(append = true)
    }

    private fun performSearch(append: Boolean = false) {
        Log.v(TAG, "performSearch() called")
        viewModelScope.launch {
            if (!append) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, offset = 0) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            try {
                val results = stationRepository.searchAndFilter(
                    query = _uiState.value.searchQuery,
                    country = _uiState.value.selectedCountry,
                    language = _uiState.value.selectedLanguage,
                    tags = _uiState.value.selectedTags,
                    limit = 30,
                    offset = _uiState.value.offset
                )

                _uiState.update { state ->
                    state.copy(
                        stations = if (append) state.stations + results else results,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = results.size == 30,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun clearError() {
        Log.v(TAG, "clearError() called")
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class HomeUiState(
    val searchQuery: String = "",
    val selectedCountry: String = "",
    val selectedLanguage: String = "",
    val selectedTags: List<String> = emptyList(),
    val stations: List<Station> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val offset: Int = 0,
    val errorMessage: String? = null,
    val availableCountries: List<String> = emptyList(),
    val availableLanguages: List<String> = emptyList(),
    val availableTags: List<TagEntity> = emptyList()
)
