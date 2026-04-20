package com.example.walls.data.analysis

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class FaceDetectionAnalyzer @Inject constructor() {

    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.08f)
                .build()
        )
    }

    // Returns face bounding boxes as normalized RectF values (0–1 relative to bitmap dimensions).
    suspend fun detectFaces(bitmap: Bitmap): List<RectF> =
        suspendCancellableCoroutine { cont ->
            try {
                val input = InputImage.fromBitmap(bitmap, 0)
                detector.process(input)
                    .addOnSuccessListener { faces ->
                        val w = bitmap.width.toFloat()
                        val h = bitmap.height.toFloat()
                        cont.resume(faces.map { face ->
                            val b = face.boundingBox
                            RectF(
                                (b.left / w).coerceIn(0f, 1f),
                                (b.top / h).coerceIn(0f, 1f),
                                (b.right / w).coerceIn(0f, 1f),
                                (b.bottom / h).coerceIn(0f, 1f)
                            )
                        })
                    }
                    .addOnFailureListener { e ->
                        Log.w("FaceDetectionAnalyzer", "Detection failed: ${e.message}")
                        cont.resume(emptyList())
                    }
            } catch (e: Exception) {
                Log.w("FaceDetectionAnalyzer", "Detection error: ${e.message}")
                cont.resume(emptyList())
            }
        }
}
