package com.example.walls.data.model

data class SmartCropSettings(
    val enabled: Boolean = false,
    val mode: SmartCropMode = SmartCropMode.AUTO,
    val previewTarget: WallpaperScreenTarget = WallpaperScreenTarget.HOME,
    val separateLockHomeFraming: Boolean = false
)
