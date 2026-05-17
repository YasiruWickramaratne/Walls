package com.example.walls.data.repository

import com.example.walls.data.local.analysis.WallpaperAnalysisDao
import com.example.walls.data.local.analysis.WallpaperAnalysisEntity
import com.example.walls.data.model.CropMetadata
import com.example.walls.data.model.SmartCropMode
import com.example.walls.data.model.WallpaperScreenTarget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperAnalysisRepository @Inject constructor(
    private val dao: WallpaperAnalysisDao
) {
    suspend fun load(
        wallpaperId: String,
        mode: SmartCropMode,
        target: WallpaperScreenTarget,
        imageWidth: Int,
        imageHeight: Int
    ): CropMetadata? {
        val entity = dao.get(cacheKey(wallpaperId, mode, target, imageWidth, imageHeight)) ?: return null
        return entity.toCropMetadata()
    }

    suspend fun save(
        metadata: CropMetadata,
        imageWidth: Int,
        imageHeight: Int
    ) {
        dao.upsert(
            WallpaperAnalysisEntity(
                cacheKey = cacheKey(metadata.wallpaperId, metadata.mode, metadata.target, imageWidth, imageHeight),
                wallpaperId = metadata.wallpaperId,
                mode = metadata.mode.name,
                target = metadata.target.persistedValue,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                leftPercent = metadata.leftPercent,
                topPercent = metadata.topPercent,
                rightPercent = metadata.rightPercent,
                bottomPercent = metadata.bottomPercent,
                score = metadata.score,
                computedAtMillis = metadata.computedAtMillis
            )
        )
    }

    suspend fun pruneOlderThan(olderThanMillis: Long) {
        dao.deleteOlderThan(olderThanMillis)
    }

    private fun WallpaperAnalysisEntity.toCropMetadata(): CropMetadata? {
        val parsedMode = runCatching { SmartCropMode.valueOf(mode) }.getOrNull() ?: return null
        return CropMetadata(
            wallpaperId = wallpaperId,
            mode = parsedMode,
            target = WallpaperScreenTarget.fromPersistedValue(target),
            leftPercent = leftPercent,
            topPercent = topPercent,
            rightPercent = rightPercent,
            bottomPercent = bottomPercent,
            score = score,
            computedAtMillis = computedAtMillis
        )
    }

    companion object {
        private fun cacheKey(
            wallpaperId: String,
            mode: SmartCropMode,
            target: WallpaperScreenTarget,
            imageWidth: Int,
            imageHeight: Int
        ): String = listOf(wallpaperId, mode.name, target.persistedValue, imageWidth, imageHeight).joinToString(":")
    }
}
