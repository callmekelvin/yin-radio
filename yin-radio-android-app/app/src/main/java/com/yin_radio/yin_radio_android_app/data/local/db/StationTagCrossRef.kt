package com.yin_radio.yin_radio_android_app.data.local.db

import androidx.room.Entity

@Entity(
    tableName = "station_tag_cross_ref",
    primaryKeys = ["stationuuid", "tagName"]
)
data class StationTagCrossRef(
    val stationuuid: String,
    val tagName: String
)
