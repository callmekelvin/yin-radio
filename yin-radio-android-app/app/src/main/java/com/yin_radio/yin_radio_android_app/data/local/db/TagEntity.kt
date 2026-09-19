package com.yin_radio.yin_radio_android_app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val tagName: String,
    val stationCount: Int
)
