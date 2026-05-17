package com.example.walls.data.analysis

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.walls.data.model.ScreenTargetProfile
import com.example.walls.data.model.SafeZoneRect
import com.example.walls.data.model.SmartCropMode
import com.example.walls.data.model.WallpaperScreenTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ScoredCrop(val rect: RectF, val score: Float)

@Singleton
class CropScorerV1 @Inject constructor(
    private val scorerV0: CropScorerV0,
    private val saliencyAnalyzer: SaliencyAnalyzer,
    private val paletteExtractor: PaletteExtractor
) {
    // Must be called from within Dispatchers.Default context.
    private fun computeScores(
        bitmap: Bitmap,
        candidates: List<RectF>,
        mode: SmartCropMode,
        profile: ScreenTargetProfile?
    ): List<ScoredCrop> {
        val saliencyMap = saliencyAnalyzer.computeSaliencyMap(bitmap)
        val visualProfile = paletteExtractor.extract(bitmap)
        return candidates.map { c ->
            val baselineScore = scorerV0.score(c, mode, profile)
            val saliencyScore = saliencyAnalyzer.scoreCrop(saliencyMap, c)
            val visualMetrics = paletteExtractor.analyzeCrop(visualProfile.colorProfile, c)
            val modeScore = modeVisualScore(mode, visualMetrics)
            val safeOverlayScore = safeOverlayScore(
                cropRect = c,
                saliencyMap = saliencyMap,
                colorProfile = visualProfile.colorProfile,
                mode = mode,
                profile = profile
            )
            val weightedScore = when (mode) {
                SmartCropMode.ICON_SAFE,
                SmartCropMode.CLOCK_SAFE -> baselineScore * 0.12f +
                    saliencyScore * 0.24f +
                    visualMetrics.paletteRelevance * 0.12f +
                    modeScore * 0.10f +
                    safeOverlayScore * 0.42f
                SmartCropMode.SUBJECT_FOCUS -> baselineScore * 0.10f +
                    saliencyScore * 0.58f +
                    visualMetrics.paletteRelevance * 0.20f +
                    modeScore * 0.12f
                SmartCropMode.SCENERY -> baselineScore * 0.20f +
                    saliencyScore * 0.30f +
                    visualMetrics.paletteRelevance * 0.18f +
                    modeScore * 0.32f
                SmartCropMode.DARK_FIT -> baselineScore * 0.14f +
                    saliencyScore * 0.26f +
                    visualMetrics.paletteRelevance * 0.14f +
                    modeScore * 0.46f
                else -> baselineScore * 0.18f +
                    saliencyScore * 0.46f +
                    visualMetrics.paletteRelevance * 0.22f +
                    modeScore * 0.14f
            }
            ScoredCrop(
                c,
                weightedScore.coerceIn(0f, 1f)
            )
        }
    }

    private fun modeVisualScore(mode: SmartCropMode, metrics: CropVisualMetrics): Float {
        return when (mode) {
            SmartCropMode.DARK_FIT -> (metrics.darkFraction * 0.70f +
                (1f - metrics.lightFraction) * 0.20f +
                metrics.vibrancy * 0.10f)
            SmartCropMode.SCENERY -> ((1f - kotlin.math.abs(metrics.brightness - 0.55f)) * 0.35f +
                metrics.saturation * 0.25f +
                metrics.paletteRelevance * 0.40f)
            SmartCropMode.SUBJECT_FOCUS -> metrics.paletteRelevance
            else -> (metrics.paletteRelevance * 0.60f +
                (1f - kotlin.math.abs(metrics.brightness - 0.50f)) * 0.25f +
                metrics.saturation * 0.15f)
        }.coerceIn(0f, 1f)
    }

    private fun safeOverlayScore(
        cropRect: RectF,
        saliencyMap: SaliencyMap,
        colorProfile: ColorProfile,
        mode: SmartCropMode,
        profile: ScreenTargetProfile?
    ): Float {
        val safeZones = profile?.safeZones ?: return 0.5f
        val zones = buildList {
            if (mode == SmartCropMode.CLOCK_SAFE || profile.target == WallpaperScreenTarget.LOCK || profile.target == WallpaperScreenTarget.BOTH) {
                safeZones.clockZone?.let { add(it to 0.58f) }
            }
            if (mode == SmartCropMode.ICON_SAFE || profile.target == WallpaperScreenTarget.HOME || profile.target == WallpaperScreenTarget.BOTH) {
                safeZones.iconZone?.let { add(it to 0.42f) }
            }
        }
        if (zones.isEmpty()) return 0.5f

        var weightedSafety = 0f
        var totalWeight = 0f
        zones.forEach { (zone, weight) ->
            val imageZone = zone.projectInto(cropRect)
            val zoneSaliency = saliencyAnalyzer.scoreCrop(saliencyMap, imageZone)
            val zoneVisuals = paletteExtractor.analyzeCrop(colorProfile, imageZone)
            val zoneActivity = (zoneSaliency * 0.62f + zoneVisuals.vibrancy * 0.24f + zoneVisuals.lightFraction * 0.14f)
                .coerceIn(0f, 1f)
            weightedSafety += (1f - zoneActivity) * weight
            totalWeight += weight
        }

        return if (totalWeight > 0f) (weightedSafety / totalWeight).coerceIn(0f, 1f) else 0.5f
    }

    private fun SafeZoneRect.projectInto(cropRect: RectF): RectF {
        val width = cropRect.width()
        val height = cropRect.height()
        return RectF(
            cropRect.left + left * width,
            cropRect.top + top * height,
            cropRect.left + right * width,
            cropRect.top + bottom * height
        )
    }

    suspend fun bestCrop(
        bitmap: Bitmap?,
        candidates: List<RectF>,
        mode: SmartCropMode = SmartCropMode.AUTO,
        profile: ScreenTargetProfile? = null
    ): ScoredCrop? =
        withContext(Dispatchers.Default) {
            if (candidates.isEmpty()) return@withContext null
            if (bitmap == null) {
                val best = scorerV0.bestCrop(candidates, mode, profile) ?: return@withContext null
                return@withContext ScoredCrop(best, scorerV0.score(best, mode, profile))
            }
            computeScores(bitmap, candidates, mode, profile).maxByOrNull { it.score }
        }

    suspend fun scoreAll(
        bitmap: Bitmap?,
        candidates: List<RectF>,
        mode: SmartCropMode = SmartCropMode.AUTO,
        profile: ScreenTargetProfile? = null
    ): List<ScoredCrop> =
        withContext(Dispatchers.Default) {
            if (candidates.isEmpty()) return@withContext emptyList()
            if (bitmap == null) return@withContext candidates.map { ScoredCrop(it, scorerV0.score(it, mode, profile)) }
            computeScores(bitmap, candidates, mode, profile)
        }
}
