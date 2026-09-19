package com.yin_radio.yin_radio_android_app.data.remote.mapper

import android.util.Log
import com.yin_radio.yin_radio_android_app.data.local.db.StationEntity
import com.yin_radio.yin_radio_android_app.data.remote.dto.StationDto

private const val TAG = "StationMapper"

fun StationDto.toEntity(): StationEntity {
    Log.v(TAG, "toEntity() called")
    return StationEntity(
        stationuuid = stationuuid,
        name = name,
        urlResolved = url_resolved,
        favicon = favicon,
        tags = tags.joinToString(","),
        country = country,
        countrycode = countrycode,
        bitrate = bitrate,
        codec = codec,
        votes = votes,
        language = language,
        languagecodes = languagecodes,
        hls = hls,
        geoLat = geo_lat,
        geoLong = geo_long
    )
}

fun StationEntity.toDomain(): com.yin_radio.yin_radio_android_app.domain.model.Station {
    Log.v(TAG, "toDomain() called")
    return com.yin_radio.yin_radio_android_app.domain.model.Station(
        stationuuid = stationuuid,
        name = name,
        urlResolved = urlResolved,
        favicon = favicon,
        tags = tags.split(",").filter { it.isNotBlank() },
        country = country,
        countrycode = countrycode,
        bitrate = bitrate,
        codec = codec,
        votes = votes,
        language = language,
        languagecodes = languagecodes,
        hls = hls,
        geoLat = geoLat,
        geoLong = geoLong,
        isFavorite = isFavorite
    )
}
