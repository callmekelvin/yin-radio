package com.yin_radio.yin_radio_android_app.data.repository

import android.util.Log
import com.yin_radio.yin_radio_android_app.data.local.dao.StationDao
import com.yin_radio.yin_radio_android_app.data.remote.mapper.toDomain
import com.yin_radio.yin_radio_android_app.domain.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FavoritesRepository(
    private val stationDao: StationDao
) {

    companion object {
        private const val TAG = "FavoritesRepository"
    }

    init {
        Log.v(TAG, "Initialized")
    }

    fun getFavoritesFlow(): Flow<List<Station>> {
        Log.v(TAG, "getFavoritesFlow() called")
        Log.v(TAG, "DAO: getFavorites()")
        return stationDao.getFavorites().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getFavorites(): List<Station> {
        Log.v(TAG, "getFavorites() called")
        return getFavoritesFlow().first()
    }

    suspend fun setFavorite(uuid: String, isFavorite: Boolean) {
        Log.v(TAG, "setFavorite() called")
        Log.v(TAG, "DAO: setFavorite(uuid=$uuid, isFavorite=$isFavorite)")
        stationDao.setFavorite(uuid, isFavorite)
    }
}
