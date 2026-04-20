package com.example.walls.data.analysis

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.walls.data.model.ScreenTargetProfile
import com.example.walls.data.model.SmartCropMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ScoredCrop(val rect: RectF, val score: Float)

@Singleton
class CropScorerV1 @Inject constructor(
    private val scorerV0: CropScorerV0,
    private val saliencyAnalyzer: SaliencyAnalyzer,
    private val paletteAnalyzer: PaletteAnalyzer
) {
    // Must be called from within Dispatchers.Default context.
    private fun computeScores(
        bitmap: Bitmap,
        candidates: List<RectF>,
        mode: SmartCropMode,
        profile: ScreenTargetProfile?
    ): List<ScoredCrop> {
        val saliencyMap = saliencyAnalyzer.computeSaliencyMap(bitmap)
        val colorProfile = paletteAnalyzer.buildColorProfile(bitmap)
        return candidates.map { c ->
            ScoredCrop(
                c,
                scorerV0.score(c, mode, profile) * 0.20f +
                    saliencyAnalyzer.scoreCrop(saliencyMap, c) * 0.55f +
                    paletteAnalyzer.scoreCrop(colorProfile, c) *
                    if (mode == SmartCropMode.DARK_FIT) 0.35f else 0.25f
            )
        }
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
