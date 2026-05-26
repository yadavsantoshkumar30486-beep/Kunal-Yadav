package com.example.database

import kotlinx.coroutines.flow.Flow

class FavoriteLocationRepository(private val dao: FavoriteLocationDao) {
    val allFavorites: Flow<List<FavoriteLocation>> = dao.getAllFavorites()

    suspend fun insert(location: FavoriteLocation) {
        dao.insertFavorite(location)
    }

    suspend fun delete(location: FavoriteLocation) {
        dao.deleteFavorite(location)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteById(id)
    }
}
