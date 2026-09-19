package com.yin_radio.yin_radio_android_app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StationDto(
    val stationuuid: String,
    val name: String,
    val url_resolved: String,
    val favicon: String? = null,
    val tags: List<String> = emptyList(),
    val country: String = "",
    val countrycode: String = "",
    val bitrate: Int = 0,
    val codec: String = "",
    val votes: Int = 0,
    val language: String = "",
    val languagecodes: String = "",
    val hls: Int = 0,
    val geo_lat: Double? = null,
    val geo_long: Double? = null
)
