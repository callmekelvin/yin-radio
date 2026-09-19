package com.yin_radio.yin_radio_android_app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.yin_radio.yin_radio_android_app.R
import com.yin_radio.yin_radio_android_app.domain.model.Station
import com.yin_radio.yin_radio_android_app.ui.components.NowPlayingBar
import com.yin_radio.yin_radio_android_app.ui.favorites.FavoritesScreen
import com.yin_radio.yin_radio_android_app.ui.home.HomeScreen
import com.yin_radio.yin_radio_android_app.ui.player.ExpandedPlayerScreen
import com.yin_radio.yin_radio_android_app.ui.settings.SettingsScreen
import org.koin.compose.viewmodel.koinViewModel

// Singleton for each Screen Configuration
sealed class Screen(val route: String, val title: String, @DrawableRes val iconRes: Int) {
    data object Home : Screen("home", "Home", R.drawable.ic_home)
    data object Favorites : Screen("favorites", "Favorites", R.drawable.ic_favorite)
    data object Settings : Screen("settings", "Settings", R.drawable.ic_settings)
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koinViewModel()
) {
    DisposableEffect(Unit) {
        Log.v("MainScreen", "Entered composition")
        onDispose { Log.v("MainScreen", "Left composition") }
    }

    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),

            // Bottom App Bar - Navigation Bar appears on all Screens, Now Playing Bar shows on Station Selection
            bottomBar = {
                Column {
                    NowPlayingBar(
                        station = uiState.currentStation,
                        onClick = { viewModel.onExpandPlayer() }
                    )

                    // Bottom Navigation Bar contains each Screen we can navigate to/ if it is the current Screen
                    NavigationBar {
                        val screens = listOf(Screen.Home, Screen.Favorites, Screen.Settings)
                        screens.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(painterResource(screen.iconRes), contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = uiState.selectedScreen == screen.route,
                                onClick = { viewModel.onScreenSelected(screen.route) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            // Show Screen within Scaffold Structure
            Box(modifier = Modifier.padding(innerPadding)) {
                // Show Screen depending on UI Screen State - With Callbacks
                when (uiState.selectedScreen) {
                    Screen.Home.route -> HomeScreen(
                        onStationClick = { station, list -> viewModel.playStation(station, list) },
                        onFavoriteClick = { viewModel.toggleFavorite(it) }
                    )
                    Screen.Favorites.route -> FavoritesScreen(
                        onStationClick = { station, list -> viewModel.playStation(station, list) },
                        onFavoriteClick = { viewModel.toggleFavorite(it) }
                    )
                    Screen.Settings.route -> SettingsScreen()
                }
            }
        }

        // Clicking Now Player Bar will expand the Player
        if (uiState.isPlayerExpanded) {
            ExpandedPlayerScreen(
                station = uiState.currentStation,
                isPlaying = uiState.isPlaying,
                isBuffering = uiState.isBuffering,
                onDismiss = { viewModel.onCollapsePlayer() },
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.playPrevious() },
                onNext = { viewModel.playNext() },
                onFavoriteClick = { uiState.currentStation?.let { viewModel.toggleFavorite(it) } }
            )
        }
    }
}
