package com.example.walls.data.analysis

import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceAwareCropScorer @Inject constructor() {

    // Scores how well the cropRect contains the detected faces.
    // Each face contributes (intersection / face area) — 1.0 = fully inside, 0.0 = not visible.
    // Returns the average coverage across all faces.
    fun score(cropRect: RectF, faces: List<RectF>): Float {
        if (faces.isEmpty()) return 0.5f // neutral when no faces detected

        var totalCoverage = 0f
        val intersect = RectF()
        for (face in faces) {
            if (intersect.setIntersect(cropRect, face)) {
                val faceArea = face.width() * face.height()
                if (faceArea > 0f) {
                    totalCoverage += (intersect.width() * intersect.height()) / faceArea
                }
            }
        }
        return (totalCoverage / faces.size).coerceIn(0f, 1f)
    }
}
