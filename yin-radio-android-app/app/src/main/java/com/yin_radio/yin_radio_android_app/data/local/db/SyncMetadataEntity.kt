package com.yin_radio.yin_radio_android_app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: Int = 1,
    val lastUpdated: String?,
    val lastSyncTimestamp: Long,
    val totalStations: Int
)
