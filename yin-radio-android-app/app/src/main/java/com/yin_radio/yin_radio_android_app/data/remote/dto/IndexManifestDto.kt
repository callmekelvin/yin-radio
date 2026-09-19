package com.yin_radio.yin_radio_android_app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class IndexManifestDto(
    val totalStations: Int,
    val pageSize: Int,
    val totalPages: Int,
    val lastUpdated: String,
    val pages: List<PageInfoDto>
)
