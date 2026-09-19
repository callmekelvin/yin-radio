package com.yin_radio.yin_radio_android_app.data.local.db

import android.util.Log
import androidx.room.Database
import androidx.room.RoomDatabase
import com.yin_radio.yin_radio_android_app.data.local.dao.StationDao
import com.yin_radio.yin_radio_android_app.data.local.dao.SyncMetadataDao
import com.yin_radio.yin_radio_android_app.data.local.dao.TagDao

@Database(
    entities = [
        StationEntity::class,
        TagEntity::class,
        StationTagCrossRef::class,
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    init {
        Log.v("AppDatabase", "Initialized")
    }

    abstract fun stationDao(): StationDao
    abstract fun tagDao(): TagDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}
