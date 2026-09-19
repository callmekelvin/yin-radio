package com.yin_radio.yin_radio_android_app.data.remote.api

import android.util.Log
import com.yin_radio.yin_radio_android_app.BuildConfig
import com.yin_radio.yin_radio_android_app.data.remote.dto.IndexManifestDto
import com.yin_radio.yin_radio_android_app.data.remote.dto.StationDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class StationsApi : IStationsApi {

    companion object {
        private const val TAG = "StationsApi"
    }

    init {
        Log.v(TAG, "Initialized")
    }

    private val baseUrl: String = BuildConfig.STATIONS_BASE_URL.removeSuffix("/")

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    override suspend fun fetchManifest(): IndexManifestDto {
        Log.v(TAG, "fetchManifest() called")
        return client.get("$baseUrl/index.json").body()
    }

    override suspend fun fetchPage(pageNumber: Int): List<StationDto> {
        Log.v(TAG, "fetchPage() called")
        return client.get("$baseUrl/$pageNumber/stations.json").body()
    }
}
