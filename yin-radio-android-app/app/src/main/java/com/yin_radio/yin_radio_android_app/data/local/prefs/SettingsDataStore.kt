package com.yin_radio.yin_radio_android_app.data.local.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

class SettingsDataStore(private val context: Context) {

    companion object {
        private const val TAG = "SettingsDataStore"
    }

    init {
        Log.v(TAG, "Initialized")
    }

    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val ALLOW_HTTP = booleanPreferencesKey("allow_http_streams")

    // Read Key Value Pairs from DataStore
    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { prefs ->
            ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }

    val allowHttpStreams: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[ALLOW_HTTP] ?: false }

    // Edit/ Set Key Value Pairs in DataStore
    suspend fun setThemeMode(mode: ThemeMode) {
        Log.v(TAG, "setThemeMode() called")
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode.name }
    }

    suspend fun setAllowHttpStreams(allowed: Boolean) {
        Log.v(TAG, "setAllowHttpStreams() called")
        context.dataStore.edit { prefs -> prefs[ALLOW_HTTP] = allowed }
    }
}
