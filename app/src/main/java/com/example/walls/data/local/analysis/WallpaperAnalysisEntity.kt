package com.example.walls.data.local.analysis

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpaper_analysis")
data class WallpaperAnalysisEntity(
    @PrimaryKey val cacheKey: String,
    val wallpaperId: String,
    val mode: String,
    val target: Int,
    val imageWidth: Int,
    val imageHeight: Int,
    val leftPercent: Float,
    val topPercent: Float,
    val rightPercent: Float,
    val bottomPercent: Float,
    val score: Float,
    val computedAtMillis: Long
)
