package com.example.walls.data.analysis

import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortraitCropPolicy @Inject constructor() {

    /**
     * Adds face-centered candidates so the scoring step has a well-positioned option to pick.
     *
     * CropCandidateGenerator slides along the longer axis of the image:
     *   - Landscape image (wider than target): slides HORIZONTALLY → cropW < 1, cropH = 1
     *   - Portrait image  (taller than target): slides VERTICALLY   → cropH < 1, cropW = 1
     *
     * The original code only adjusted the top edge, which helped portrait but was a no-op for
     * landscape since (1f - 1f) = 0 clamped every candidate to the same RectF.
     */
    fun augmentCandidates(baseCandidates: List<RectF>, faces: List<RectF>): List<RectF> {
        if (faces.isEmpty() || baseCandidates.isEmpty()) return baseCandidates

        val faceLeft   = faces.minOf { it.left }
        val faceRight  = faces.maxOf { it.right }
        val faceTop    = faces.minOf { it.top }
        val faceBottom = faces.maxOf { it.bottom }
        val faceCenterX = (faceLeft + faceRight) / 2f
        val faceHeight  = faceBottom - faceTop

        val prototype = baseCandidates.first()
        val cropW = prototype.width()
        val cropH = prototype.height()

        val extras = mutableListOf<RectF>()

        // Portrait image: slides vertically (cropH < 1, cropW ≈ 1)
        if (cropH < 0.999f) {
            val headroom = (faceHeight * 0.25f).coerceIn(0.02f, cropH * 0.25f)
            val top = (faceTop - headroom).coerceIn(0f, (1f - cropH).coerceAtLeast(0f))
            extras.add(RectF(prototype.left, top, prototype.right, (top + cropH).coerceAtMost(1f)))
        }

        // Landscape image: slides horizontally (cropW < 1, cropH ≈ 1)
        if (cropW < 0.999f) {
            val left = (faceCenterX - cropW / 2f).coerceIn(0f, (1f - cropW).coerceAtLeast(0f))
            extras.add(RectF(left, prototype.top, (left + cropW).coerceAtMost(1f), prototype.bottom))
        }

        return if (extras.isEmpty()) baseCandidates else baseCandidates + extras
    }
}
