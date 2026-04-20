package com.example.walls.data.manager

import android.content.Context
import com.example.walls.data.model.SmartCropMode
import com.example.walls.data.model.SmartCropSettings
import com.example.walls.data.model.WallpaperScreenTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartCropSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "WallsPrefs"
        private const val KEY_SMART_FIT_ENABLED = "SMART_FIT_ENABLED"
        private const val KEY_SMART_FIT_MODE = "SMART_FIT_MODE"
        private const val KEY_SMART_FIT_TARGET = "SMART_FIT_TARGET"
        private const val KEY_SMART_FIT_SEPARATE = "SMART_FIT_SEPARATE"
    }

    fun isSmartFitEnabled(): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SMART_FIT_ENABLED, false)
    }

    fun setSmartFitEnabled(enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SMART_FIT_ENABLED, enabled)
            .apply()
    }

    fun loadSettings(): SmartCropSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return SmartCropSettings(
            enabled = prefs.getBoolean(KEY_SMART_FIT_ENABLED, false),
            mode = runCatching {
                SmartCropMode.valueOf(prefs.getString(KEY_SMART_FIT_MODE, SmartCropMode.AUTO.name) ?: SmartCropMode.AUTO.name)
            }.getOrDefault(SmartCropMode.AUTO),
            previewTarget = WallpaperScreenTarget.fromPersistedValue(
                prefs.getInt(KEY_SMART_FIT_TARGET, WallpaperScreenTarget.HOME.persistedValue)
            ),
            separateLockHomeFraming = prefs.getBoolean(KEY_SMART_FIT_SEPARATE, false)
        )
    }

    fun saveSettings(settings: SmartCropSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SMART_FIT_ENABLED, settings.enabled)
            .putString(KEY_SMART_FIT_MODE, settings.mode.name)
            .putInt(KEY_SMART_FIT_TARGET, settings.previewTarget.persistedValue)
            .putBoolean(KEY_SMART_FIT_SEPARATE, settings.separateLockHomeFraming)
            .apply()
    }
}
