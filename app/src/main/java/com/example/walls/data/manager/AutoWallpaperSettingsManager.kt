package com.example.walls.data.manager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.walls.data.model.AutoWallpaperConfig
import com.example.walls.data.model.AutoWallpaperHistoryEntry
import com.example.walls.data.model.RotationSource
import com.example.walls.data.model.WallpaperScreenTarget
import com.example.walls.worker.AutoWallpaperWorker
import com.example.walls.worker.WallpaperAnalysisWorker
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoWallpaperSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "WallsPrefs"
        private const val KEY_ENABLED = "AUTO_CHANGE_ENABLED"
        private const val KEY_INTERVAL = "AUTO_CHANGE_INTERVAL"
        private const val KEY_SCREEN = "WALLPAPER_SCREEN"
        private const val KEY_SOURCE = "AUTO_WALLPAPER_SOURCE"
        private const val KEY_COLLECTIONS = "AUTO_WALLPAPER_COLLECTIONS"
        private const val KEY_HISTORY = "AUTO_WALLPAPER_HISTORY"
        private const val UNIQUE_WORK_NAME = "auto_wallpaper_change"
        private const val UNIQUE_ANALYSIS_WORK_NAME = "wallpaper_analysis_precompute"
    }

    private val gson = Gson()

    fun loadConfig(): AutoWallpaperConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val persistedSource = prefs.getString(KEY_SOURCE, RotationSource.FAVORITES.name)
        val rotationSource = runCatching { RotationSource.valueOf(persistedSource ?: RotationSource.FAVORITES.name) }
            .getOrDefault(RotationSource.FAVORITES)
        val selectedSources = prefs.getStringSet(
            KEY_COLLECTIONS,
            setOf(AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION)
        )?.toSet().orEmpty().ifEmpty { setOf(AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION) }

        return AutoWallpaperConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            intervalMs = prefs.getLong(KEY_INTERVAL, 15 * 60 * 1000L),
            screenTarget = WallpaperScreenTarget.fromPersistedValue(prefs.getInt(KEY_SCREEN, WallpaperScreenTarget.HOME.persistedValue)),
            rotationSource = rotationSource,
            selectedSources = selectedSources
        )
    }

    fun saveConfig(config: AutoWallpaperConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putLong(KEY_INTERVAL, config.intervalMs)
            .putInt(KEY_SCREEN, config.screenTarget.persistedValue)
            .putString(KEY_SOURCE, config.rotationSource.name)
            .putStringSet(KEY_COLLECTIONS, config.selectedSources.toSet())
            .apply()
    }

    fun scheduleIfEnabled(config: AutoWallpaperConfig = loadConfig()) {
        if (!config.enabled) {
            cancel()
            return
        }

        val intervalMinutes = config.intervalMs / (60 * 1000)
        if (intervalMinutes < 5) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(
            intervalMinutes,
            TimeUnit.MINUTES,
            5,
            TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        val analysisRequest = PeriodicWorkRequestBuilder<WallpaperAnalysisWorker>(
            intervalMinutes,
            TimeUnit.MINUTES,
            5,
            TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_ANALYSIS_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            analysisRequest
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_ANALYSIS_WORK_NAME)
    }

    fun loadLatestHistory(): AutoWallpaperHistoryEntry? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return null
        return runCatching {
            gson.fromJson(json, AutoWallpaperHistoryEntry::class.java)
        }.getOrNull()
    }

    fun recordHistory(entry: AutoWallpaperHistoryEntry) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, gson.toJson(entry))
            .apply()
    }
}
