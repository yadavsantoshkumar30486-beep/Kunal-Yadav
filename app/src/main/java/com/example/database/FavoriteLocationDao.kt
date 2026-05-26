package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteLocationDao {
    @Query("SELECT * FROM favorite_locations ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(location: FavoriteLocation)

    @Delete
    suspend fun deleteFavorite(location: FavoriteLocation)

    @Query("DELETE FROM favorite_locations WHERE id = :id")
    suspend fun deleteById(id: Int)
}
