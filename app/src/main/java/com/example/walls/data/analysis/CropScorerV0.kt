package com.example.walls.data.analysis

import android.graphics.RectF
import com.example.walls.data.model.ScreenTargetProfile
import com.example.walls.data.model.SmartCropMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class CropScorerV0 @Inject constructor() {

    fun score(
        cropRect: RectF,
        mode: SmartCropMode = SmartCropMode.AUTO,
        profile: ScreenTargetProfile? = null
    ): Float {
        val cx = (cropRect.left + cropRect.right) / 2f
        val cy = (cropRect.top + cropRect.bottom) / 2f
        // Higher score when crop center is near image center (0.5, 0.5)
        var score = 1f - (abs(cx - 0.5f) + abs(cy - 0.5f))

        val safeZones = profile?.safeZones
        if (safeZones != null) {
            safeZones.clockZone?.toRectF()?.let { clock ->
                if (RectF.intersects(cropRect, clock)) {
                    score -= if (mode == SmartCropMode.CLOCK_SAFE || profile.target.name == "LOCK") 0.2f else 0.08f
                }
            }
            safeZones.iconZone?.toRectF()?.let { icons ->
                if (RectF.intersects(cropRect, icons)) {
                    score -= if (mode == SmartCropMode.ICON_SAFE || profile.target.name == "HOME") 0.18f else 0.06f
                }
            }
        }

        if (mode == SmartCropMode.SCENERY) {
            score += (1f - abs(cy - 0.45f)) * 0.08f
        }

        return score.coerceIn(0f, 1f)
    }

    fun bestCrop(
        candidates: List<RectF>,
        mode: SmartCropMode = SmartCropMode.AUTO,
        profile: ScreenTargetProfile? = null
    ): RectF? = candidates.maxByOrNull { score(it, mode, profile) }
}
