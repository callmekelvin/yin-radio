package com.yin_radio.yin_radio_android_app.ui.main

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.yin_radio.yin_radio_android_app.data.local.prefs.SettingsDataStore
import com.yin_radio.yin_radio_android_app.data.repository.FavoritesRepository
import com.yin_radio.yin_radio_android_app.data.repository.StationRepository
import com.yin_radio.yin_radio_android_app.domain.model.Station
import com.yin_radio.yin_radio_android_app.service.RadioPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedScreen: String = Screen.Home.route,
    val currentStation: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isPlayerExpanded: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(
    application: Application,
    private val stationRepository: StationRepository,
    private val favoritesRepository: FavoritesRepository,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private var currentResultSet: List<Station> = emptyList()
    private var currentIndex: Int = -1

    // Initialize the MediaController to connect with the RadioPlaybackService
    // This allows the ViewModel to control playback and observe player state
    init {
        Log.v(TAG, "Initialized")
        // Create a session token that identifies the RadioPlaybackService
        val sessionToken = SessionToken(
            application,
            ComponentName(application, RadioPlaybackService::class.java)
        )

        // Build the MediaController asynchronously
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()

        controllerFuture.addListener({
            // Once connected, store the controller and attach a listener for state changes
            mediaController = controllerFuture.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _uiState.update {
                        it.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
                    }
                }
            })
        }, MoreExecutors.directExecutor())
    }

    // Callback Functions to update UI State for Main UI
    fun onScreenSelected(route: String) {
        Log.v(TAG, "onScreenSelected() called")
        _uiState.update { it.copy(selectedScreen = route) }
    }

    fun onExpandPlayer() {
        Log.v(TAG, "onExpandPlayer() called")
        _uiState.update { it.copy(isPlayerExpanded = true) }
    }

    fun onCollapsePlayer() {
        Log.v(TAG, "onCollapsePlayer() called")
        _uiState.update { it.copy(isPlayerExpanded = false) }
    }

    fun playStation(station: Station, fromList: List<Station> = emptyList()) {
        Log.v(TAG, "playStation() called")
        viewModelScope.launch {
            // Reads the SettingDataStore Model AllowHTTP Current Value at Point in Time within Coroutine (not within Composable Function)
            val allowHttp = settingsDataStore.allowHttpStreams.first()
            if (station.urlResolved.startsWith("http://") && !allowHttp) {
                _uiState.update { it.copy(errorMessage = "This station requires HTTP playback. Enable it in Settings.") }
                return@launch
            }

            // Create Media Item for Media Controller/ Playback
            val mediaItem = MediaItem.Builder()
                .setUri(station.urlResolved)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.name)
                        .build()
                )
                .build()
            mediaController?.setMediaItem(mediaItem)
            mediaController?.prepare()
            mediaController?.play()

            // Set Current Result and Radio Index -> Update Main State UI
            currentResultSet = fromList
            currentIndex = fromList.indexOfFirst { it.stationuuid == station.stationuuid }

            _uiState.update { it.copy(currentStation = station, isPlaying = true, errorMessage = null) }
        }
    }

    // Toggling between Pause and Play
    fun togglePlayPause() {
        Log.v(TAG, "togglePlayPause() called")
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    // Play Previous Station based off Station List Index
    fun playPrevious() {
        Log.v(TAG, "playPrevious() called")
        if (currentResultSet.isEmpty() || currentIndex <= 0) {
            _uiState.update { it.copy(errorMessage = "No previous station available.") }
            return
        }

        currentIndex--
        val station = currentResultSet[currentIndex]

        val mediaItem = MediaItem.Builder()
            .setUri(station.urlResolved)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(station.name).build())
            .build()
        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()

        _uiState.update { it.copy(currentStation = station, isPlaying = true, errorMessage = null) }
    }

    // Play Next Station based off Station List Index
    fun playNext() {
        Log.v(TAG, "playNext() called")
        if (currentResultSet.isEmpty() || currentIndex >= currentResultSet.lastIndex) {
            _uiState.update { it.copy(errorMessage = "No next station available.") }
            return
        }

        currentIndex++
        val station = currentResultSet[currentIndex]

        val mediaItem = MediaItem.Builder()
            .setUri(station.urlResolved)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(station.name).build())
            .build()
        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()

        _uiState.update { it.copy(currentStation = station, isPlaying = true, errorMessage = null) }
    }

    // Set Favourite Radio Stations
    fun toggleFavorite(station: Station) {
        Log.v(TAG, "toggleFavorite() called")
        // Set Favourite Radio Stations Model using Coroutines
        viewModelScope.launch {
            favoritesRepository.setFavorite(station.stationuuid, !station.isFavorite)

            // Only update currentStation if it's the same station being favourited
            _uiState.update {
                if (it.currentStation?.stationuuid == station.stationuuid) {
                    it.copy(
                        currentStation = it.currentStation?.copy(isFavorite = !station.isFavorite)
                    )
                }
                else {
                    // Leave as is
                    it
                }
            }
        }
    }

    fun clearError() {
        Log.v(TAG, "clearError() called")
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        Log.v(TAG, "onCleared() called")
        mediaController?.release()
        super.onCleared()
    }
}
