package com.yin_radio.yin_radio_android_app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yin_radio.yin_radio_android_app.data.local.prefs.SettingsDataStore
import com.yin_radio.yin_radio_android_app.data.local.prefs.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        Log.v(TAG, "Initialized")
        viewModelScope.launch {
            settingsDataStore.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.allowHttpStreams.collect { allowed ->
                _uiState.update { it.copy(allowHttpStreams = allowed) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        Log.v(TAG, "setThemeMode() called")
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setAllowHttpStreams(allowed: Boolean) {
        Log.v(TAG, "setAllowHttpStreams() called")
        viewModelScope.launch {
            settingsDataStore.setAllowHttpStreams(allowed)
        }
    }

}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val allowHttpStreams: Boolean = false
)
