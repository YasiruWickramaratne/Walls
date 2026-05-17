package com.example.walls.data.local.analysis

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WallpaperAnalysisDao {
    @Query("SELECT * FROM wallpaper_analysis WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): WallpaperAnalysisEntity?

    @Query("SELECT * FROM wallpaper_analysis WHERE wallpaperId = :wallpaperId")
    suspend fun getForWallpaper(wallpaperId: String): List<WallpaperAnalysisEntity>

    @Upsert
    suspend fun upsert(entity: WallpaperAnalysisEntity)

    @Query("DELETE FROM wallpaper_analysis WHERE wallpaperId = :wallpaperId")
    suspend fun deleteForWallpaper(wallpaperId: String)

    @Query("DELETE FROM wallpaper_analysis WHERE computedAtMillis < :olderThanMillis")
    suspend fun deleteOlderThan(olderThanMillis: Long)
}
