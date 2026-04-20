package com.example.walls.data.analysis

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.walls.data.model.CollectionStylePreset
import com.example.walls.data.model.ScreenTargetProfile
import com.example.walls.data.model.SmartCropMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CropScorerV2 @Inject constructor(
    private val scorerV1: CropScorerV1,
    private val faceDetectionAnalyzer: FaceDetectionAnalyzer,
    private val faceAwareCropScorer: FaceAwareCropScorer,
    private val portraitCropPolicy: PortraitCropPolicy,
    private val collectionAwareCropScorer: CollectionAwareCropScorer
) {
    suspend fun bestCrop(
        bitmap: Bitmap?,
        candidates: List<RectF>,
        mode: SmartCropMode = SmartCropMode.AUTO,
        profile: ScreenTargetProfile? = null,
        stylePreset: CollectionStylePreset = CollectionStylePreset.DEFAULT
    ): ScoredCrop? =
        withContext(Dispatchers.Default) {
            if (candidates.isEmpty()) return@withContext null
            if (bitmap == null) return@withContext scorerV1.bestCrop(null, candidates, mode, profile)

            val faces = faceDetectionAnalyzer.detectFaces(bitmap)
            Log.d("CropScorerV2", "Faces detected: ${faces.size} - $faces")

            if (faces.isEmpty()) {
                return@withContext scorerV1.bestCrop(bitmap, candidates, mode, profile)
            }

            val augmented = portraitCropPolicy.augmentCandidates(candidates, faces)
            val v1Scores = scorerV1.scoreAll(bitmap, augmented, mode, profile)

            // Score every candidate for face coverage first so we can adapt the blend weights.
            val faceScores = v1Scores.map { faceAwareCropScorer.score(it.rect, faces) }
            val maxFaceCoverage = faceScores.max()

            Log.d("CropScorerV2", "maxFaceCoverage=$maxFaceCoverage candidates=${augmented.size}")

            // Adaptive weights: when coverage is good, trust face more;
            // when coverage is weak (face near edge / too large), still prefer it over V1.
            val (wV1, wFace) = when {
                maxFaceCoverage >= 0.5f -> 0.25f to 0.75f
                maxFaceCoverage > 0.0f  -> 0.15f to 0.85f
                else -> return@withContext scorerV1.bestCrop(bitmap, candidates, mode, profile)
            }

            v1Scores.zip(faceScores).map { (scored, faceScore) ->
                val mergedScore = scored.score * wV1 + faceScore * wFace
                val styledScore = collectionAwareCropScorer.applyStyle(mergedScore, scored.score, stylePreset)
                ScoredCrop(scored.rect, styledScore)
            }.maxByOrNull { it.score }
        }
}
