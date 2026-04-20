package com.example.walls.data.analysis

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton

class ColorProfile(val values: FloatArray, val width: Int, val height: Int)

@Singleton
class PaletteAnalyzer @Inject constructor() {

    companion object {
        private const val PROFILE_SIZE = 128
    }

    fun buildColorProfile(bitmap: Bitmap): ColorProfile {
        val scale = PROFILE_SIZE.toFloat() / bitmap.width.coerceAtLeast(bitmap.height)
        val sw = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val sh = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, true)

        val pixels = IntArray(sw * sh)
        scaled.getPixels(pixels, 0, sw, 0, 0, sw, sh)
        scaled.recycle()

        val hsv = FloatArray(3)
        // saturation × value approximates perceived colorfulness
        val vibrancy = FloatArray(sw * sh) { i ->
            Color.colorToHSV(pixels[i] or -0x1000000, hsv)
            hsv[1] * hsv[2]
        }
        return ColorProfile(vibrancy, sw, sh)
    }

    fun scoreCrop(profile: ColorProfile, cropRect: RectF): Float {
        val x0 = (cropRect.left * profile.width).toInt().coerceIn(0, profile.width - 1)
        val x1 = (cropRect.right * profile.width).toInt().coerceIn(x0 + 1, profile.width)
        val y0 = (cropRect.top * profile.height).toInt().coerceIn(0, profile.height - 1)
        val y1 = (cropRect.bottom * profile.height).toInt().coerceIn(y0 + 1, profile.height)

        var sum = 0f
        var count = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                sum += profile.values[y * profile.width + x]
                count++
            }
        }
        return if (count > 0) (sum / count).coerceIn(0f, 1f) else 0f
    }
}
