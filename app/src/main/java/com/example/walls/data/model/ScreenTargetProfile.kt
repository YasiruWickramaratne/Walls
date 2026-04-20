package com.example.walls.data.model

import android.graphics.RectF

data class SafeZoneRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toRectF(): RectF = RectF(left, top, right, bottom)
}

data class WallpaperSafeZones(
    val topFraction: Float = 0.04f,
    val bottomFraction: Float = 0.06f,
    val aspectRatio: Float,
    val clockZone: SafeZoneRect? = null,
    val iconZone: SafeZoneRect? = null
)

data class ScreenTargetProfile(
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val safeZones: WallpaperSafeZones,
    val target: WallpaperScreenTarget
) {
    val aspectRatio: Float get() = screenWidthPx.toFloat() / screenHeightPx
}
