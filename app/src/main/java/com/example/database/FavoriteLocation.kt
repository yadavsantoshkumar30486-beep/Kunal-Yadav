package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_locations")
data class FavoriteLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val presetId: String, // associated preset scenario ID (e.g. hurricane_hera, tornado_vortex)
    val note: String = "Monitoring Active Radar Core",
    val customTemp: Int = 72,
    val timestamp: Long = System.currentTimeMillis()
)
