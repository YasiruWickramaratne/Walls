package com.example.walls.worker

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.walls.data.analysis.SmartFitCropResolver
import com.example.walls.data.manager.AutoWallpaperSettingsManager
import com.example.walls.data.manager.FavoritesCollectionManager
import com.example.walls.data.manager.SmartCropSettingsManager
import com.example.walls.data.model.CollectionStylePreset
import com.example.walls.data.model.RotationSource
import com.example.walls.data.model.WallpaperScreenTarget
import com.example.walls.data.repository.WallpaperAnalysisRepository
import com.example.walls.data.repository.WallpaperRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@HiltWorker
class WallpaperAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val favoritesCollectionManager: FavoritesCollectionManager,
    private val wallpaperRepository: WallpaperRepository,
    private val autoWallpaperSettingsManager: AutoWallpaperSettingsManager,
    private val smartCropSettingsManager: SmartCropSettingsManager,
    private val smartFitCropResolver: SmartFitCropResolver,
    private val analysisRepository: WallpaperAnalysisRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val smartSettings = smartCropSettingsManager.loadSettings()
            if (!smartSettings.enabled) return@withContext Result.success()

            val config = autoWallpaperSettingsManager.loadConfig()
            val apiKey = wallpaperRepository.getApiKey()
            val wallpaperIds = when (config.rotationSource) {
                RotationSource.COLLECTIONS -> {
                    val resolvedSources = config.selectedSources.ifEmpty {
                        setOf(com.example.walls.data.model.AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION)
                    }
                    buildSet {
                        if (resolvedSources.any {
                                it.equals(
                                    com.example.walls.data.model.AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION,
                                    ignoreCase = true
                                )
                            }) {
                            addAll(favoritesCollectionManager.getFavoriteIds())
                        }
                        addAll(
                            favoritesCollectionManager.getCollectionWallpaperIds(
                                resolvedSources.filterNot {
                                    it.equals(
                                        com.example.walls.data.model.AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION,
                                        ignoreCase = true
                                    )
                                }.toSet()
                            )
                        )
                    }
                }
                RotationSource.FAVORITES -> favoritesCollectionManager.getFavoriteIds()
            }

            val wallpapers = favoritesCollectionManager.fetchWallpapersByIds(apiKey, wallpaperIds)
            val targets = if (smartSettings.separateLockHomeFraming || config.screenTarget == WallpaperScreenTarget.BOTH) {
                listOf(WallpaperScreenTarget.HOME, WallpaperScreenTarget.LOCK)
            } else {
                listOf(config.screenTarget)
            }

            wallpapers.forEach { wallpaper ->
                val bitmap = downloadBitmap(wallpaper.path) ?: return@forEach
                targets.forEach { target ->
                    smartFitCropResolver.analyze(
                        wallpaperId = wallpaper.id,
                        bitmap = bitmap,
                        mode = smartSettings.mode,
                        target = target,
                        stylePreset = CollectionStylePreset.DEFAULT
                    )
                }
                bitmap.recycle()
            }

            analysisRepository.pruneOlderThan(System.currentTimeMillis() - CACHE_TTL_MILLIS)
            Result.success()
        } catch (e: Exception) {
            Log.e("WallpaperAnalysisWorker", "Analysis precompute failed", e)
            Result.retry()
        }
    }

    private fun downloadBitmap(imageUrl: String): android.graphics.Bitmap? {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        return try {
            connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                requestMethod = "GET"
            }
            inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.w("WallpaperAnalysisWorker", "Could not decode bitmap: ${e.message}")
            null
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
    }

    companion object {
        private const val CACHE_TTL_MILLIS = 30L * 24L * 60L * 60L * 1000L
    }
}
