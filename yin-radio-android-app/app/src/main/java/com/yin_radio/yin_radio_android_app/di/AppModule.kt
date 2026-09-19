package com.yin_radio.yin_radio_android_app.di

import android.app.Application
import android.util.Log
import androidx.media3.session.MediaSessionService
import androidx.room.Room
import com.yin_radio.yin_radio_android_app.data.local.db.AppDatabase
import com.yin_radio.yin_radio_android_app.data.local.prefs.SettingsDataStore
import com.yin_radio.yin_radio_android_app.data.remote.api.StationsApi
import com.yin_radio.yin_radio_android_app.data.remote.api.IStationsApi
import com.yin_radio.yin_radio_android_app.data.repository.FavoritesRepository
import com.yin_radio.yin_radio_android_app.data.repository.StationRepository
import com.yin_radio.yin_radio_android_app.service.RadioPlaybackService
import com.yin_radio.yin_radio_android_app.ui.favorites.FavoritesViewModel
import com.yin_radio.yin_radio_android_app.ui.home.HomeViewModel
import com.yin_radio.yin_radio_android_app.ui.main.MainViewModel
import com.yin_radio.yin_radio_android_app.ui.sync.StationSyncViewModel
import com.yin_radio.yin_radio_android_app.ui.player.PlayerViewModel
import com.yin_radio.yin_radio_android_app.ui.settings.SettingsViewModel
import org.koin.plugin.module.dsl.*
import org.koin.dsl.module
import org.koin.dsl.bind

fun provideDatabase(app: Application): AppDatabase =
    Room.databaseBuilder(app, AppDatabase::class.java, "yin_radio.db")
        .setQueryCallback({ sqlQuery, bindArgs ->
            Log.v("Room", "SQL: $sqlQuery | args: $bindArgs")
        }, java.util.concurrent.Executor { it.run() })
        .build()
fun provideStationDao(db: AppDatabase) = db.stationDao()
fun provideTagDao(db: AppDatabase) = db.tagDao()
fun provideSyncMetadataDao(db: AppDatabase) = db.syncMetadataDao()

val appModule = module {
    // Singleton: Room Database
    single { create(::provideDatabase) }

    // Singleton: DAOs retrieved from the database instance
    single { create(::provideStationDao) }
    single { create(::provideTagDao) }
    single { create(::provideSyncMetadataDao) }

    // Compiler plugin auto-wires constructor parameters from the graph
    single<SettingsDataStore>()

    // Singleton: API Implementation
    single<StationsApi>() bind IStationsApi::class

    // Singleton: Repositories
    single<StationRepository>()
    single<FavoritesRepository>()

    // ViewModels (scoped to the Activity/Fragment lifecycle)
    viewModel<StationSyncViewModel>()
    viewModel<MainViewModel>()
    viewModel<HomeViewModel>()
    viewModel<FavoritesViewModel>()
    viewModel<PlayerViewModel>()
    viewModel<SettingsViewModel>()
}
