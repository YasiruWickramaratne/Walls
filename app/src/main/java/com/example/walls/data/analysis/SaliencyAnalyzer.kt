package com.example.walls.data.analysis

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class SaliencyMap(val values: FloatArray, val width: Int, val height: Int)

@Singleton
class SaliencyAnalyzer @Inject constructor() {

    companion object {
        private const val MAP_WIDTH = 96
    }

    fun computeSaliencyMap(bitmap: Bitmap): SaliencyMap {
        val w = MAP_WIDTH
        val h = (MAP_WIDTH.toFloat() * bitmap.height / bitmap.width).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val openCvMap = computeOpenCvSaliencyMap(scaled)
        if (openCvMap != null) {
            scaled.recycle()
            return openCvMap
        }

        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)
        scaled.recycle()

        val lum = FloatArray(w * h) { i ->
            val p = pixels[i]
            (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)) / 255f
        }

        val sal = FloatArray(w * h)
        var maxVal = 1e-6f
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val center = lum[y * w + x]
                var diff = 0f
                for (dy in -1..1) for (dx in -1..1) {
                    if (dx != 0 || dy != 0) diff += abs(center - lum[(y + dy) * w + (x + dx)])
                }
                sal[y * w + x] = diff / 8f
                if (sal[y * w + x] > maxVal) maxVal = sal[y * w + x]
            }
        }
        for (i in sal.indices) sal[i] /= maxVal

        return SaliencyMap(sal, w, h)
    }

    private fun computeOpenCvSaliencyMap(scaled: Bitmap): SaliencyMap? {
        var rgba: Mat? = null
        var gray: Mat? = null
        var gray32: Mat? = null
        var localAverage: Mat? = null
        var globalAverage: Mat? = null
        var saliency: Mat? = null
        var normalized: Mat? = null
        var saliency8: Mat? = null
        return try {
            rgba = Mat()
            gray = Mat()
            gray32 = Mat()
            localAverage = Mat()
            globalAverage = Mat()
            saliency = Mat()
            normalized = Mat()
            saliency8 = Mat()
            Utils.bitmapToMat(scaled, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            gray.convertTo(gray32, CvType.CV_32F, 1.0 / 255.0)

            Imgproc.GaussianBlur(gray32, localAverage, Size(0.0, 0.0), 1.2)
            Imgproc.GaussianBlur(gray32, globalAverage, Size(0.0, 0.0), 8.0)
            Core.absdiff(localAverage, globalAverage, saliency)
            Core.normalize(saliency, normalized, 0.0, 255.0, Core.NORM_MINMAX)
            normalized.convertTo(saliency8, CvType.CV_8U)

            val values = ByteArray(saliency8.total().toInt())
            saliency8.get(0, 0, values)
            SaliencyMap(
                values = FloatArray(values.size) { i -> (values[i].toInt() and 0xFF) / 255f },
                width = saliency8.cols(),
                height = saliency8.rows()
            )
        } catch (_: Throwable) {
            null
        } finally {
            rgba?.release()
            gray?.release()
            gray32?.release()
            localAverage?.release()
            globalAverage?.release()
            saliency?.release()
            normalized?.release()
            saliency8?.release()
        }
    }

    fun scoreCrop(map: SaliencyMap, cropRect: RectF): Float {
        val x0 = (cropRect.left * map.width).toInt().coerceIn(0, map.width - 1)
        val x1 = (cropRect.right * map.width).toInt().coerceIn(x0 + 1, map.width)
        val y0 = (cropRect.top * map.height).toInt().coerceIn(0, map.height - 1)
        val y1 = (cropRect.bottom * map.height).toInt().coerceIn(y0 + 1, map.height)

        var sum = 0f
        var count = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                sum += map.values[y * map.width + x]
                count++
            }
        }
        return if (count > 0) sum / count else 0f
    }
}
