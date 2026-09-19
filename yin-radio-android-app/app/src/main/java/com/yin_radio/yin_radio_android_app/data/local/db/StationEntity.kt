package com.yin_radio.yin_radio_android_app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val stationuuid: String,
    val name: String,
    val urlResolved: String,
    val favicon: String?,
    val tags: String,
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
    val isFavorite: Boolean = false
)
