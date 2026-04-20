package com.example.walls.data.manager

import android.content.res.Resources
import com.example.walls.data.model.SafeZoneRect
import com.example.walls.data.model.ScreenTargetProfile
import com.example.walls.data.model.WallpaperScreenTarget
import com.example.walls.data.model.WallpaperSafeZones
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenProfileManager @Inject constructor() {

    fun getProfile(target: WallpaperScreenTarget = WallpaperScreenTarget.HOME): ScreenTargetProfile {
        val dm = Resources.getSystem().displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val safeZones = when (target) {
            WallpaperScreenTarget.LOCK -> WallpaperSafeZones(
                topFraction = 0.04f,
                bottomFraction = 0.05f,
                aspectRatio = w.toFloat() / h,
                clockZone = SafeZoneRect(0.16f, 0.08f, 0.84f, 0.28f)
            )
            WallpaperScreenTarget.BOTH -> WallpaperSafeZones(
                topFraction = 0.04f,
                bottomFraction = 0.08f,
                aspectRatio = w.toFloat() / h,
                clockZone = SafeZoneRect(0.16f, 0.08f, 0.84f, 0.26f),
                iconZone = SafeZoneRect(0.08f, 0.70f, 0.92f, 0.94f)
            )
            WallpaperScreenTarget.HOME -> WallpaperSafeZones(
                topFraction = 0.03f,
                bottomFraction = 0.08f,
                aspectRatio = w.toFloat() / h,
                iconZone = SafeZoneRect(0.08f, 0.70f, 0.92f, 0.94f)
            )
        }
        return ScreenTargetProfile(
            screenWidthPx = w,
            screenHeightPx = h,
            safeZones = safeZones,
            target = target
        )
    }
}
