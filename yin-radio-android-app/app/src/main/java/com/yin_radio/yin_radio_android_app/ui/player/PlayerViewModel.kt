package com.yin_radio.yin_radio_android_app.ui.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yin_radio.yin_radio_android_app.data.local.prefs.SettingsDataStore
import com.yin_radio.yin_radio_android_app.data.repository.FavoritesRepository
import com.yin_radio.yin_radio_android_app.data.repository.StationRepository
import com.yin_radio.yin_radio_android_app.domain.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val stationRepository: StationRepository,
    private val favoritesRepository: FavoritesRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    companion object {
        private const val TAG = "PlayerViewModel"
    }

    init {
        Log.v(TAG, "Initialized")
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun updateStation(station: Station?) {
        Log.v(TAG, "updateStation() called")
        _uiState.update { it.copy(station = station) }
    }

    fun toggleFavorite() {
        Log.v(TAG, "toggleFavorite() called")
        val station = _uiState.value.station ?: return
        viewModelScope.launch {
            favoritesRepository.setFavorite(station.stationuuid, !station.isFavorite)
            _uiState.update { it.copy(station = station.copy(isFavorite = !station.isFavorite)) }
        }
    }
}

data class PlayerUiState(
    val station: Station? = null
)
