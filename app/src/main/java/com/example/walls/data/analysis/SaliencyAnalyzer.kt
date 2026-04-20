package com.example.walls.data.analysis

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

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
