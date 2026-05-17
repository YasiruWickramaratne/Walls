package com.example.walls.data.analysis

import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CropCandidateGenerator @Inject constructor() {

    fun generate(
        imageWidth: Int,
        imageHeight: Int,
        targetAspect: Float,
        steps: Int = 15
    ): List<RectF> {
        if (imageWidth <= 0 || imageHeight <= 0 || targetAspect <= 0f) return emptyList()

        val imageAspect = imageWidth.toFloat() / imageHeight
        val stepCount = steps.coerceAtLeast(1)
        val candidates = mutableListOf<RectF>()

        if (imageAspect >= targetAspect) {
            // Wider than target: slide horizontally, full height
            val cropWidth = imageHeight * targetAspect
            val maxLeft = imageWidth - cropWidth
            for (i in 0 until stepCount) {
                val left = if (stepCount == 1) maxLeft / 2f else maxLeft * i / (stepCount - 1)
                candidates.add(RectF(
                    left / imageWidth,
                    0f,
                    (left + cropWidth) / imageWidth,
                    1f
                ))
            }
        } else {
            // Taller than target: slide vertically, full width
            val cropHeight = imageWidth / targetAspect
            val maxTop = imageHeight - cropHeight
            for (i in 0 until stepCount) {
                val top = if (stepCount == 1) maxTop / 2f else maxTop * i / (stepCount - 1)
                candidates.add(RectF(
                    0f,
                    top / imageHeight,
                    1f,
                    (top + cropHeight) / imageHeight
                ))
            }
        }
        return candidates
    }
}
