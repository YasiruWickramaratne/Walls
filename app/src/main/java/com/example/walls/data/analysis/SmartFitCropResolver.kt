package com.example.walls.data.analysis

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.walls.data.manager.CropMetadataManager
import com.example.walls.data.manager.ScreenProfileManager
import com.example.walls.data.manager.SmartCropSettingsManager
import com.example.walls.data.model.CollectionStylePreset
import com.example.walls.data.model.CropMetadata
import com.example.walls.data.model.SmartCropMode
import com.example.walls.data.model.WallpaperScreenTarget
import com.example.walls.data.repository.WallpaperAnalysisRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartFitCropResolver @Inject constructor(
    private val smartCropSettingsManager: SmartCropSettingsManager,
    private val screenProfileManager: ScreenProfileManager,
    private val cropCandidateGenerator: CropCandidateGenerator,
    private val cropScorerV2: CropScorerV2,
    private val cropMetadataManager: CropMetadataManager,
    private val analysisRepository: WallpaperAnalysisRepository
) {
    suspend fun resolve(
        wallpaperId: String,
        bitmap: Bitmap,
        target: WallpaperScreenTarget,
        stylePreset: CollectionStylePreset = CollectionStylePreset.DEFAULT
    ): CropMetadata? {
        val settings = smartCropSettingsManager.loadSettings()
        if (!settings.enabled) return null

        val mode = settings.mode.normalizedForAnalysis()
        val resolvedTarget = if (settings.previewTarget == WallpaperScreenTarget.BOTH) target else settings.previewTarget
        val cached = analysisRepository.load(
            wallpaperId = wallpaperId,
            mode = mode,
            target = resolvedTarget,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
        if (cached != null) return cached

        return analyze(
            wallpaperId = wallpaperId,
            bitmap = bitmap,
            mode = mode,
            target = resolvedTarget,
            stylePreset = stylePreset
        )
    }

    suspend fun analyze(
        wallpaperId: String,
        bitmap: Bitmap,
        mode: SmartCropMode,
        target: WallpaperScreenTarget,
        stylePreset: CollectionStylePreset = CollectionStylePreset.DEFAULT
    ): CropMetadata? {
        val profile = screenProfileManager.getProfile(target)
        val candidates = cropCandidateGenerator.generate(bitmap.width, bitmap.height, profile.aspectRatio)
        val scored = cropScorerV2.bestCrop(
            bitmap = bitmap,
            candidates = candidates,
            mode = mode.normalizedForAnalysis(),
            profile = profile,
            stylePreset = stylePreset
        ) ?: return null

        val metadata = CropMetadata(
            wallpaperId = wallpaperId,
            mode = mode.normalizedForAnalysis(),
            target = target,
            leftPercent = scored.rect.left,
            topPercent = scored.rect.top,
            rightPercent = scored.rect.right,
            bottomPercent = scored.rect.bottom,
            score = scored.score
        )
        analysisRepository.save(metadata, bitmap.width, bitmap.height)
        cropMetadataManager.save(metadata)
        return metadata
    }

    fun toPixelRect(metadata: CropMetadata, bitmap: Bitmap): Rect {
        val left = (metadata.leftPercent * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (metadata.topPercent * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (metadata.rightPercent * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (metadata.bottomPercent * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        return Rect(left, top, right, bottom)
    }

    private fun SmartCropMode.normalizedForAnalysis(): SmartCropMode {
        return if (this == SmartCropMode.MANUAL || this == SmartCropMode.SMART_FIT) SmartCropMode.AUTO else this
    }
}
