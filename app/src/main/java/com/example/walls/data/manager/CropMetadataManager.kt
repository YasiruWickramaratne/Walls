package com.example.walls.data.manager

import android.content.Context
import com.example.walls.data.model.CropMetadata
import com.example.walls.data.model.SmartCropMode
import com.example.walls.data.model.WallpaperScreenTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CropMetadataManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "WallsPrefs"
        private const val PREFIX = "smart_crop_"
    }

    fun save(metadata: CropMetadata) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString("${PREFIX}mode_${metadata.wallpaperId}", metadata.mode.name)
            putFloat("${PREFIX}left_${metadata.wallpaperId}", metadata.leftPercent)
            putFloat("${PREFIX}top_${metadata.wallpaperId}", metadata.topPercent)
            putFloat("${PREFIX}right_${metadata.wallpaperId}", metadata.rightPercent)
            putFloat("${PREFIX}bottom_${metadata.wallpaperId}", metadata.bottomPercent)
            putFloat("${PREFIX}score_${metadata.wallpaperId}", metadata.score)
            putInt("${PREFIX}target_${metadata.wallpaperId}", metadata.target.persistedValue)
            putLong("${PREFIX}at_${metadata.wallpaperId}", metadata.computedAtMillis)
            apply()
        }
    }

    fun load(wallpaperId: String): CropMetadata? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString("${PREFIX}mode_$wallpaperId", null) ?: return null
        val mode = runCatching { SmartCropMode.valueOf(modeStr) }.getOrNull() ?: return null
        return CropMetadata(
            wallpaperId = wallpaperId,
            mode = mode,
            leftPercent = prefs.getFloat("${PREFIX}left_$wallpaperId", 0f),
            topPercent = prefs.getFloat("${PREFIX}top_$wallpaperId", 0f),
            rightPercent = prefs.getFloat("${PREFIX}right_$wallpaperId", 1f),
            bottomPercent = prefs.getFloat("${PREFIX}bottom_$wallpaperId", 1f),
            score = prefs.getFloat("${PREFIX}score_$wallpaperId", 0f),
            target = WallpaperScreenTarget.fromPersistedValue(
                prefs.getInt("${PREFIX}target_$wallpaperId", WallpaperScreenTarget.HOME.persistedValue)
            ),
            computedAtMillis = prefs.getLong("${PREFIX}at_$wallpaperId", 0L)
        )
    }

    fun has(wallpaperId: String): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains("${PREFIX}mode_$wallpaperId")
    }

    fun delete(wallpaperId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            remove("${PREFIX}mode_$wallpaperId")
            remove("${PREFIX}left_$wallpaperId")
            remove("${PREFIX}top_$wallpaperId")
            remove("${PREFIX}right_$wallpaperId")
            remove("${PREFIX}bottom_$wallpaperId")
            remove("${PREFIX}score_$wallpaperId")
            remove("${PREFIX}target_$wallpaperId")
            remove("${PREFIX}at_$wallpaperId")
            apply()
        }
    }
}
