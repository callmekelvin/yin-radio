package com.yin_radio.yin_radio_android_app.domain.model

data class Station(
    val stationuuid: String,
    val name: String,
    val urlResolved: String,
    val favicon: String?,
    val tags: List<String>,
    val country: String,
    val countrycode: String,
    val bitrate: Int,
    val codec: String,
    val votes: Int,
    val language: String,
    val languagecodes: String,
    val hls: Int,
    val geoLat: Double?,
    val geoLong: Double?,
    val isFavorite: Boolean
)
