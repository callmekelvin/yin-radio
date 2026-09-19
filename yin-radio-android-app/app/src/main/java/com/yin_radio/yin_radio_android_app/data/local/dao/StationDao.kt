package com.yin_radio.yin_radio_android_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.yin_radio.yin_radio_android_app.data.local.db.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stations: List<StationEntity>)

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE name LIKE :query ORDER BY votes DESC LIMIT :limit OFFSET :offset")
    suspend fun searchByName(query: String, limit: Int, offset: Int): List<StationEntity>

    @Query("SELECT * FROM stations WHERE country = :country ORDER BY votes DESC LIMIT :limit OFFSET :offset")
    suspend fun filterByCountry(country: String, limit: Int, offset: Int): List<StationEntity>

    @Query("SELECT * FROM stations WHERE languagecodes LIKE :language ORDER BY votes DESC LIMIT :limit OFFSET :offset")
    suspend fun filterByLanguage(language: String, limit: Int, offset: Int): List<StationEntity>

    @Query("SELECT * FROM stations WHERE tags LIKE :tag ORDER BY votes DESC LIMIT :limit OFFSET :offset")
    suspend fun filterByTag(tag: String, limit: Int, offset: Int): List<StationEntity>

    @Query("""
        SELECT * FROM stations 
        WHERE name LIKE :query 
        AND country LIKE :country 
        AND languagecodes LIKE :language 
        AND tags LIKE :tag 
        ORDER BY votes DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchAndFilter(
        query: String,
        country: String,
        language: String,
        tag: String,
        limit: Int,
        offset: Int
    ): List<StationEntity>

    @Query("UPDATE stations SET isFavorite = :isFavorite WHERE stationuuid = :uuid")
    suspend fun setFavorite(uuid: String, isFavorite: Boolean)

    @Query("DELETE FROM stations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM stations")
    suspend fun getCount(): Int

    @Query("SELECT * FROM stations WHERE stationuuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): StationEntity?

    @Query("SELECT DISTINCT country FROM stations WHERE country != '' ORDER BY country ASC")
    suspend fun getDistinctCountries(): List<String>

    @Query("SELECT DISTINCT languagecodes FROM stations WHERE languagecodes != '' ORDER BY languagecodes ASC")
    suspend fun getDistinctLanguageCodes(): List<String>

    @RawQuery
    suspend fun searchAndFilterRaw(query: SupportSQLiteQuery): List<StationEntity>
}
