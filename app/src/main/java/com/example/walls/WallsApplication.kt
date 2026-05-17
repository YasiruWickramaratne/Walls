package com.example.walls

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.disk.DiskCache
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class WallsApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        if (OpenCVLoader.initLocal()) {
            Log.i("WallsApplication", "OpenCV loaded successfully")
        } else {
            Log.w("WallsApplication", "OpenCV initialization failed; using Kotlin image-analysis fallback")
        }

        WorkManager.initialize(this, workManagerConfiguration)

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizeBytes(48 * 1024 * 1024) // 48MB: fits ~48 cards at 540x960 RGB565
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(150L * 1024 * 1024)
                        .build()
                }
                .allowHardware(false)
                .allowRgb565(true)
                .respectCacheHeaders(false)
                .crossfade(false)
                .build()
        )
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
