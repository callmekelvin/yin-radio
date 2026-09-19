package com.yin_radio.yin_radio_android_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yin_radio.yin_radio_android_app.data.local.db.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Query("SELECT * FROM tags ORDER BY stationCount DESC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("DELETE FROM tags")
    suspend fun clearAll()
}
