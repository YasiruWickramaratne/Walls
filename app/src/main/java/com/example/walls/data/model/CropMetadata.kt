package com.example.walls.data.model

data class CropMetadata(
    val wallpaperId: String,
    val mode: SmartCropMode,
    val target: WallpaperScreenTarget = WallpaperScreenTarget.HOME,
    val leftPercent: Float,
    val topPercent: Float,
    val rightPercent: Float,
    val bottomPercent: Float,
    val score: Float = 0f,
    val computedAtMillis: Long = System.currentTimeMillis()
)
