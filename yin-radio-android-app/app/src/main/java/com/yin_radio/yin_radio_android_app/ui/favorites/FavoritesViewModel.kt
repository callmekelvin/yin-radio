package com.yin_radio.yin_radio_android_app.ui.favorites

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yin_radio.yin_radio_android_app.data.local.prefs.SettingsDataStore
import com.yin_radio.yin_radio_android_app.data.repository.FavoritesRepository
import com.yin_radio.yin_radio_android_app.domain.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    companion object {
        private const val TAG = "FavoritesViewModel"
    }

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        Log.v(TAG, "Initialized")
        viewModelScope.launch {
            favoritesRepository.getFavoritesFlow().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites, isLoading = false) }
            }
        }
    }

    fun toggleFavorite(station: Station) {
        Log.v(TAG, "toggleFavorite() called")
        viewModelScope.launch {
            favoritesRepository.setFavorite(station.stationuuid, !station.isFavorite)
        }
    }

}

data class FavoritesUiState(
    val favorites: List<Station> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
