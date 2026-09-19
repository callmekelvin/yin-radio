package com.yin_radio.yin_radio_android_app.data.remote.api

import com.yin_radio.yin_radio_android_app.data.remote.dto.IndexManifestDto
import com.yin_radio.yin_radio_android_app.data.remote.dto.StationDto

interface IStationsApi {
    suspend fun fetchManifest(): IndexManifestDto
    suspend fun fetchPage(pageNumber: Int): List<StationDto>
}