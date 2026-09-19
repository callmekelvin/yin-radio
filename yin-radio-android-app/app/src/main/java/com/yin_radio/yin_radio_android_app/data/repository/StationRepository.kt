package com.yin_radio.yin_radio_android_app.data.repository

import com.yin_radio.yin_radio_android_app.data.local.dao.StationDao
import com.yin_radio.yin_radio_android_app.data.local.dao.SyncMetadataDao
import com.yin_radio.yin_radio_android_app.data.local.dao.TagDao
import com.yin_radio.yin_radio_android_app.data.local.db.StationEntity
import com.yin_radio.yin_radio_android_app.data.local.db.SyncMetadataEntity
import com.yin_radio.yin_radio_android_app.data.local.db.TagEntity
import com.yin_radio.yin_radio_android_app.data.remote.api.StationsApi
import com.yin_radio.yin_radio_android_app.data.remote.mapper.toDomain
import com.yin_radio.yin_radio_android_app.data.remote.mapper.toEntity
import com.yin_radio.yin_radio_android_app.domain.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery

class StationRepository(
    private val api: StationsApi,
    private val stationDao: StationDao,
    private val tagDao: TagDao,
    private val syncMetadataDao: SyncMetadataDao
) {

    companion object {
        private const val TAG = "StationRepository"
    }

    init {
        Log.v(TAG, "Initialized")
    }

    suspend fun syncStations(): Result<Unit> {
        Log.v(TAG, "syncStations() called")
        return try {

            // Fetch API Manifest Page
            val manifest = api.fetchManifest()
            val allStations = mutableListOf<StationEntity>()

            for (pageInfo in manifest.pages) {
                // Fetch Stations from Each Page
                val page = api.fetchPage(pageInfo.page)

                // Map Stations from Each Page from StationEntity DTO to StationEntity
                allStations.addAll(page.map { it.toEntity() })
            }

            // Compute tag counts from Stations
            val tagCounts = mutableMapOf<String, Int>()
            allStations.forEach { station ->
                station.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                    tagCounts[tag] = tagCounts.getOrDefault(tag, 0) + 1
                }
            }
            val tags = tagCounts.map { (name, count) -> TagEntity(name, count) }

            // All-or-nothing write - Clear Existing and Insert All New Station Data
            Log.v(TAG, "DAO: clearAll() stations")
            stationDao.clearAll()
            Log.v(TAG, "DAO: clearAll() tags")
            tagDao.clearAll()
            Log.v(TAG, "DAO: insertAll(count=${allStations.size}) stations")
            stationDao.insertAll(allStations)
            Log.v(TAG, "DAO: insertAll(count=${tags.size}) tags")
            tagDao.insertAll(tags)

            Log.v(TAG, "DAO: insert() syncMetadata")
            syncMetadataDao.insert(
                SyncMetadataEntity(
                    lastUpdated = manifest.lastUpdated,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    totalStations = allStations.size
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLastSyncMetadata(): SyncMetadataEntity? {
        Log.v(TAG, "getLastSyncMetadata() called")
        return syncMetadataDao.get()
    }

    suspend fun hasData(): Boolean {
        Log.v(TAG, "hasData() called")
        Log.v(TAG, "DAO: getCount()")
        return stationDao.getCount() > 0
    }

    suspend fun searchAndFilter(
        query: String = "",
        country: String = "",
        language: String = "",
        tags: List<String> = emptyList(),
        limit: Int = 30,
        offset: Int = 0
    ): List<Station> {
        Log.v(TAG, "searchAndFilter() called")
        val q = if (query.isBlank()) "%%" else "%${query.lowercase()}%"
        val c = if (country.isBlank()) "%%" else country
        val l = if (language.isBlank()) "%%" else "%${language.lowercase()}%"

        val args = mutableListOf<Any>(q, c, l)

        val tagClause = if (tags.isEmpty()) {
            "tags LIKE '%%'"
        } else {
            tags.joinToString(separator = " OR ") {
                args.add("%${it.lowercase()}%")
                "tags LIKE ?"
            }
        }

        val sql = """
            SELECT * FROM stations 
            WHERE name LIKE ? 
            AND country LIKE ? 
            AND languagecodes LIKE ? 
            AND ($tagClause) 
            ORDER BY votes DESC 
            LIMIT ? OFFSET ?
        """.trimIndent()

        args.add(limit)
        args.add(offset)

        Log.v(TAG, "DAO: searchAndFilterRaw() - sql: $sql, args: $args")
        return stationDao.searchAndFilterRaw(SimpleSQLiteQuery(sql, args.toTypedArray())).map { it.toDomain() }
    }

    fun getFavorites(): Flow<List<Station>> {
        Log.v(TAG, "getFavorites() called")
        Log.v(TAG, "DAO: getFavorites()")
        return stationDao.getFavorites().map { list -> list.map { it.toDomain() } }
    }

    suspend fun setFavorite(uuid: String, isFavorite: Boolean) {
        Log.v(TAG, "setFavorite() called")
        Log.v(TAG, "DAO: setFavorite(uuid=$uuid, isFavorite=$isFavorite)")
        stationDao.setFavorite(uuid, isFavorite)
    }

    suspend fun getStationByUuid(uuid: String): Station? {
        Log.v(TAG, "getStationByUuid() called")
        Log.v(TAG, "DAO: getByUuid(uuid=$uuid)")
        return stationDao.getByUuid(uuid)?.toDomain()
    }

    suspend fun getDistinctCountries(): List<String> {
        Log.v(TAG, "getDistinctCountries() called")
        Log.v(TAG, "DAO: getDistinctCountries()")
        return stationDao.getDistinctCountries()
    }

    suspend fun getDistinctLanguageCodes(): List<String> {
        Log.v(TAG, "getDistinctLanguageCodes() called")
        Log.v(TAG, "DAO: getDistinctLanguageCodes()")
        return stationDao.getDistinctLanguageCodes()
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun getAllTags(): Flow<List<TagEntity>> {
        Log.v(TAG, "getAllTags() called")
        Log.v(TAG, "DAO: getAllTags()")
        return tagDao.getAllTags()
    }
}
