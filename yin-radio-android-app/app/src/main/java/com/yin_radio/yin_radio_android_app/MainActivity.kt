package com.yin_radio.yin_radio_android_app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yin_radio.yin_radio_android_app.data.local.prefs.SettingsDataStore
import com.yin_radio.yin_radio_android_app.data.local.prefs.ThemeMode
import com.yin_radio.yin_radio_android_app.data.repository.StationRepository
import com.yin_radio.yin_radio_android_app.ui.main.MainScreen
import com.yin_radio.yin_radio_android_app.ui.onboarding.OnboardingScreen
import com.yin_radio.yin_radio_android_app.ui.theme.YinRadioTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    // DI Injection
    private val settingsDataStore: SettingsDataStore by inject()
    private val stationRepository: StationRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v("MainActivity", "onCreate()")

        // Allow Content to extend past System Bars
        enableEdgeToEdge()

        // Define Compose UI
        setContent {
            // Subscribe to changes in Theme from Persistent DataStore
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            // Retain Boolean State of Data across recompilation
            var hasData by remember { mutableStateOf<Boolean?>(null) }

            // Lifecycle Event (Side Effect - Launches coroutine to load data in on app startup)
            LaunchedEffect(Unit) {
                hasData = stationRepository.hasData()
            }

            YinRadioTheme(themeMode = themeMode) {
                // Load different Composable Functions depending on if Station Data is present in Room DB
                when (hasData) {
                    null -> { /* Loading state, blank */ }

                    // Display Onboarding Screen if we need to populate data from the Source (First App Launch)
                    // Pass Callback Function to set hasData to True when data is populated from Source
                    false -> OnboardingScreen(onComplete = { hasData = true })

                    // Display Main Screen
                    true -> MainScreen()
                }
            }
        }
    }
}
