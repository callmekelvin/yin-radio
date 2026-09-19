package com.yin_radio.yin_radio_android_app.ui.sync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yin_radio.yin_radio_android_app.data.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StationSyncUiState(
    val isSyncing: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
    val lastSyncTimestamp: Long? = null
)

class StationSyncViewModel(
    private val stationRepository: StationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "StationSyncViewModel"
    }

    private val _uiState = MutableStateFlow(StationSyncUiState())
    val uiState: StateFlow<StationSyncUiState> = _uiState.asStateFlow()

    init {
        Log.v(TAG, "Initialized")
        loadLastSyncMetadata()
    }

    fun startSync() {
        if (_uiState.value.isSyncing) {
            Log.v(TAG, "startSync() ignored: already syncing")
            return
        }

        Log.v(TAG, "startSync() called")
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSyncing = true, isComplete = false, errorMessage = null)
            }

            val result = stationRepository.syncStations()

            if (result.isSuccess) {
                Log.v(TAG, "syncStations() succeeded")
                _uiState.update { it.copy(isSyncing = false, isComplete = true) }
                loadLastSyncMetadata()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Sync failed"
                Log.e(TAG, "syncStations() failed: $error")
                _uiState.update { it.copy(isSyncing = false, errorMessage = error) }
            }
        }
    }

    fun reset() {
        val current = _uiState.value
        if (current.isSyncing) {
            Log.v(TAG, "reset() ignored: sync in progress")
            return
        }
        Log.v(TAG, "reset() called")
        _uiState.value = StationSyncUiState(lastSyncTimestamp = current.lastSyncTimestamp)
    }

    private fun loadLastSyncMetadata() {
        viewModelScope.launch {
            val metadata = stationRepository.getLastSyncMetadata()
            _uiState.update { it.copy(lastSyncTimestamp = metadata?.lastSyncTimestamp) }
        }
    }
}
