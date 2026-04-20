package com.example.walls.worker

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.walls.data.manager.AutoWallpaperSettingsManager
import com.example.walls.data.manager.FavoritesCollectionManager
import com.example.walls.data.model.AutoWallpaperConfig
import com.example.walls.data.model.AutoWallpaperHistoryEntry
import com.example.walls.data.model.RotationSource
import com.example.walls.data.repository.WallpaperRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@HiltWorker
class AutoWallpaperWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val favoritesCollectionManager: FavoritesCollectionManager,
    private val wallpaperRepository: WallpaperRepository,
    private val autoWallpaperSettingsManager: AutoWallpaperSettingsManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AutoWallpaperWorker", "doWork() started")
        
        return try {
            val context = applicationContext
            val autoWallpaperConfig = autoWallpaperSettingsManager.loadConfig()
            
            val apiKey = wallpaperRepository.getApiKey()
            val wallpaperIds = when (autoWallpaperConfig.rotationSource) {
                RotationSource.COLLECTIONS -> {
                    val resolvedSources = if (autoWallpaperConfig.selectedSources.isEmpty()) {
                        setOf(AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION)
                    } else {
                        autoWallpaperConfig.selectedSources
                    }
                    buildSet {
                        if (resolvedSources.any { it.equals(AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION, ignoreCase = true) }) {
                            addAll(favoritesCollectionManager.getFavoriteIds())
                        }
                        val collectionNames = resolvedSources
                            .filterNot { it.equals(AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION, ignoreCase = true) }
                            .toSet()
                        addAll(favoritesCollectionManager.getCollectionWallpaperIds(collectionNames))
                    }
                }
                else -> favoritesCollectionManager.getFavoriteIds()
            }
            val favoriteWallpapers = favoritesCollectionManager.fetchWallpapersByIds(apiKey, wallpaperIds)

            Log.d("AutoWallpaperWorker", "Fetched ${favoriteWallpapers.size} favorite wallpapers")

            if (favoriteWallpapers.isEmpty()) {
                Log.d("AutoWallpaperWorker", "No favorite wallpapers found")
                Result.retry()
            } else {
                // Get a random wallpaper
                val wallpaperToSet = favoriteWallpapers.random()
                Log.d("AutoWallpaperWorker", "Selected wallpaper: ${wallpaperToSet.id}")

                setWallpaper(
                    context = context,
                    imageUrl = wallpaperToSet.path,
                    wallpaperId = wallpaperToSet.id,
                    thumbnailUrl = wallpaperToSet.thumbs.small,
                    screen = autoWallpaperConfig.screenTarget.persistedValue
                )
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("AutoWallpaperWorker", "Error in doWork()", e)
            Result.retry()
        } finally {
            Log.d("AutoWallpaperWorker", "doWork() finished")
        }
    }

    private suspend fun setWallpaper(
        context: Context,
        imageUrl: String,
        wallpaperId: String,
        thumbnailUrl: String,
        screen: Int
    ) {
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            try {
                val url = URL(imageUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val croppedBitmap = cropBitmap(context, bitmap, wallpaperId)
                    val applied = setWallpaperToScreen(context, croppedBitmap, screen)
                    if (applied) {
                        autoWallpaperSettingsManager.recordHistory(
                            AutoWallpaperHistoryEntry(
                                wallpaperId = wallpaperId,
                                wallpaperName = "Wallpaper $wallpaperId",
                                thumbnailUrl = thumbnailUrl,
                                changedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    Log.e("AutoWallpaperWorker", "Bitmap decoding failed")
                }
            } catch (e: IOException) {
                Log.e("AutoWallpaperWorker", "Error setting wallpaper", e)
            } finally {
                inputStream?.close()
                connection?.disconnect()
            }
        }
    }

    private fun cropBitmap(context: Context, fullBitmap: Bitmap, wallpaperId: String): Bitmap {
        val sharedPref = context.getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)

        // Sprint 6: SmartFitCropResolver will check CropMetadataManager here and override
        // percentages when background analysis has produced a SMART_FIT entry. Until then,
        // manual crop percentages from CropActivity are used as-is.

        // Get the relative percentages
        val leftPercent = sharedPref.getFloat("crop_rect_left_percent_$wallpaperId", 0f)
        val topPercent = sharedPref.getFloat("crop_rect_top_percent_$wallpaperId", 0f)
        val rightPercent = sharedPref.getFloat("crop_rect_right_percent_$wallpaperId", 1f)
        val bottomPercent = sharedPref.getFloat("crop_rect_bottom_percent_$wallpaperId", 1f)
        
        // Calculate actual pixels based on current bitmap dimensions
        val left = (leftPercent * fullBitmap.width).toInt()
        val top = (topPercent * fullBitmap.height).toInt()
        val right = (rightPercent * fullBitmap.width).toInt()
        val bottom = (bottomPercent * fullBitmap.height).toInt()

        if (leftPercent > 0f || topPercent > 0f || rightPercent < 1f || bottomPercent < 1f) {
            try {
                val cropRect = Rect(left, top, right, bottom)
                return Bitmap.createBitmap(
                    fullBitmap,
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height()
                )
            } catch (e: Exception) {
                Log.e("AutoWallpaperWorker", "Error cropping bitmap", e)
                return fullBitmap
            }
        }
        return fullBitmap
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setWallpaperToScreen(context: Context, bitmap: Bitmap, screen: Int): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(context)
        try {
            when (screen) {
                0 -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM) // Home screen
                1 -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)   // Lock screen
                2 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) // Both
                } else {
                    wallpaperManager.setBitmap(bitmap) // Fallback for older versions
                }
            }
            Log.d("AutoWallpaperWorker", "Wallpaper set successfully to screen: $screen")
            return true
        } catch (e: IOException) {
            Log.e("AutoWallpaperWorker", "Error setting wallpaper", e)
            return false
        }
    }
} 
