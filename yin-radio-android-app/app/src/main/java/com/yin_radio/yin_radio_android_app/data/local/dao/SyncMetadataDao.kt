package com.yin_radio.yin_radio_android_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yin_radio.yin_radio_android_app.data.local.db.SyncMetadataEntity

@Dao
interface SyncMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE id = 1")
    suspend fun get(): SyncMetadataEntity?
}
