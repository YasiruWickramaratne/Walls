package com.example.walls.data.analysis

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

data class DominantColor(
    val argb: Int,
    val population: Float,
    val hue: Float,
    val saturation: Float,
    val brightness: Float,
    val warmCoolScore: Float
)

data class ImageVisualProfile(
    val colorProfile: ColorProfile,
    val dominantColors: List<DominantColor>,
    val averageBrightness: Float,
    val averageSaturation: Float,
    val warmCoolScore: Float,
    val darkPixelFraction: Float,
    val lightPixelFraction: Float,
    val vibrantScore: Float
) {
    val darkLightBalance: Float get() = averageBrightness
}

data class CropVisualMetrics(
    val brightness: Float,
    val saturation: Float,
    val warmCoolScore: Float,
    val darkFraction: Float,
    val lightFraction: Float,
    val vibrancy: Float,
    val paletteRelevance: Float
)

class ColorProfile(
    val brightness: FloatArray,
    val saturation: FloatArray,
    val warmth: FloatArray,
    val vibrancy: FloatArray,
    val width: Int,
    val height: Int
)

@Singleton
class PaletteExtractor @Inject constructor() {

    companion object {
        private const val PROFILE_SIZE = 128
        private const val K_CLUSTERS = 5
        private const val KMEANS_ITERATIONS = 8
    }

    fun extract(bitmap: Bitmap): ImageVisualProfile {
        val scale = PROFILE_SIZE.toFloat() / bitmap.width.coerceAtLeast(bitmap.height)
        val sw = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val sh = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, true)

        val pixels = IntArray(sw * sh)
        scaled.getPixels(pixels, 0, sw, 0, 0, sw, sh)
        scaled.recycle()

        val hsv = FloatArray(3)
        val brightness = FloatArray(sw * sh)
        val saturation = FloatArray(sw * sh)
        val warmth = FloatArray(sw * sh)
        val vibrancy = FloatArray(sw * sh)

        var brightnessSum = 0f
        var saturationSum = 0f
        var warmthSum = 0f
        var darkCount = 0
        var lightCount = 0
        var vibrancySum = 0f

        for (i in pixels.indices) {
            Color.colorToHSV(pixels[i] or -0x1000000, hsv)
            val value = hsv[2].coerceIn(0f, 1f)
            val sat = hsv[1].coerceIn(0f, 1f)
            val warm = hueWarmth(hsv[0]) * sat
            val vib = sat * value

            brightness[i] = value
            saturation[i] = sat
            warmth[i] = warm
            vibrancy[i] = vib

            brightnessSum += value
            saturationSum += sat
            warmthSum += warm
            vibrancySum += vib
            if (value < 0.32f) darkCount++
            if (value > 0.72f) lightCount++
        }

        val count = pixels.size.coerceAtLeast(1)
        return ImageVisualProfile(
            colorProfile = ColorProfile(
                brightness = brightness,
                saturation = saturation,
                warmth = warmth,
                vibrancy = vibrancy,
                width = sw,
                height = sh
            ),
            dominantColors = extractDominantColors(pixels),
            averageBrightness = brightnessSum / count,
            averageSaturation = saturationSum / count,
            warmCoolScore = (warmthSum / count).coerceIn(-1f, 1f),
            darkPixelFraction = darkCount.toFloat() / count,
            lightPixelFraction = lightCount.toFloat() / count,
            vibrantScore = (vibrancySum / count).coerceIn(0f, 1f)
        )
    }

    fun buildColorProfile(bitmap: Bitmap): ColorProfile = extract(bitmap).colorProfile

    fun scoreCrop(profile: ColorProfile, cropRect: RectF): Float {
        return analyzeCrop(profile, cropRect).paletteRelevance
    }

    fun analyzeCrop(profile: ColorProfile, cropRect: RectF): CropVisualMetrics {
        val x0 = (cropRect.left * profile.width).toInt().coerceIn(0, profile.width - 1)
        val x1 = (cropRect.right * profile.width).toInt().coerceIn(x0 + 1, profile.width)
        val y0 = (cropRect.top * profile.height).toInt().coerceIn(0, profile.height - 1)
        val y1 = (cropRect.bottom * profile.height).toInt().coerceIn(y0 + 1, profile.height)

        var brightnessSum = 0f
        var saturationSum = 0f
        var warmthSum = 0f
        var vibrancySum = 0f
        var darkCount = 0
        var lightCount = 0
        var count = 0

        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val index = y * profile.width + x
                val brightness = profile.brightness[index]
                brightnessSum += brightness
                saturationSum += profile.saturation[index]
                warmthSum += profile.warmth[index]
                vibrancySum += profile.vibrancy[index]
                if (brightness < 0.32f) darkCount++
                if (brightness > 0.72f) lightCount++
                count++
            }
        }

        if (count == 0) {
            return CropVisualMetrics(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }

        val brightness = brightnessSum / count
        val saturation = saturationSum / count
        val vibrancy = (vibrancySum / count).coerceIn(0f, 1f)
        val darkFraction = darkCount.toFloat() / count
        val lightFraction = lightCount.toFloat() / count
        val contrast = abs(lightFraction - darkFraction)
        val relevance = (vibrancy * 0.62f + saturation * 0.18f + contrast * 0.20f).coerceIn(0f, 1f)

        return CropVisualMetrics(
            brightness = brightness.coerceIn(0f, 1f),
            saturation = saturation.coerceIn(0f, 1f),
            warmCoolScore = (warmthSum / count).coerceIn(-1f, 1f),
            darkFraction = darkFraction,
            lightFraction = lightFraction,
            vibrancy = vibrancy,
            paletteRelevance = relevance
        )
    }

    private fun extractDominantColors(pixels: IntArray): List<DominantColor> {
        if (pixels.isEmpty()) return emptyList()
        val sampleStep = (pixels.size / 1600).coerceAtLeast(1)
        val samples = pixels.filterIndexed { index, _ -> index % sampleStep == 0 }
            .map { it or -0x1000000 }
        if (samples.isEmpty()) return emptyList()

        val centers = initialCenters(samples, K_CLUSTERS)
        val assignments = IntArray(samples.size)
        repeat(KMEANS_ITERATIONS) {
            for (i in samples.indices) {
                assignments[i] = nearestCenter(samples[i], centers)
            }
            val sums = Array(centers.size) { FloatArray(3) }
            val counts = IntArray(centers.size)
            for (i in samples.indices) {
                val cluster = assignments[i]
                sums[cluster][0] += Color.red(samples[i])
                sums[cluster][1] += Color.green(samples[i])
                sums[cluster][2] += Color.blue(samples[i])
                counts[cluster]++
            }
            for (i in centers.indices) {
                if (counts[i] > 0) {
                    centers[i] = Color.rgb(
                        (sums[i][0] / counts[i]).toInt().coerceIn(0, 255),
                        (sums[i][1] / counts[i]).toInt().coerceIn(0, 255),
                        (sums[i][2] / counts[i]).toInt().coerceIn(0, 255)
                    )
                }
            }
        }

        val counts = IntArray(centers.size)
        for (sample in samples) counts[nearestCenter(sample, centers)]++

        val hsv = FloatArray(3)
        return centers.indices
            .filter { counts[it] > 0 }
            .sortedByDescending { counts[it] }
            .map { index ->
                Color.colorToHSV(centers[index], hsv)
                DominantColor(
                    argb = centers[index],
                    population = counts[index].toFloat() / samples.size,
                    hue = hsv[0],
                    saturation = hsv[1],
                    brightness = hsv[2],
                    warmCoolScore = hueWarmth(hsv[0]) * hsv[1]
                )
            }
    }

    private fun initialCenters(samples: List<Int>, k: Int): IntArray {
        val sorted = samples.sortedBy { luminance(it) }
        val clusterCount = min(k, sorted.size)
        return IntArray(clusterCount) { i ->
            sorted[((i + 0.5f) * sorted.size / clusterCount).toInt().coerceIn(sorted.indices)]
        }
    }

    private fun nearestCenter(color: Int, centers: IntArray): Int {
        var bestIndex = 0
        var bestDistance = Float.MAX_VALUE
        for (i in centers.indices) {
            val distance = colorDistance(color, centers[i])
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = i
            }
        }
        return bestIndex
    }

    private fun colorDistance(a: Int, b: Int): Float {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return sqrt((dr * dr + dg * dg + db * db).toFloat())
    }

    private fun luminance(color: Int): Float {
        return (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f
    }

    private fun hueWarmth(hue: Float): Float {
        val normalized = ((hue % 360f) + 360f) % 360f
        val warmDistance = min(abs(normalized - 35f), 360f - abs(normalized - 35f)) / 180f
        val coolDistance = min(abs(normalized - 215f), 360f - abs(normalized - 215f)) / 180f
        return (coolDistance - warmDistance).coerceIn(-1f, 1f)
    }
}

@Singleton
class PaletteAnalyzer @Inject constructor(
    private val paletteExtractor: PaletteExtractor
) {
    fun buildColorProfile(bitmap: Bitmap): ColorProfile = paletteExtractor.buildColorProfile(bitmap)

    fun extract(bitmap: Bitmap): ImageVisualProfile = paletteExtractor.extract(bitmap)

    fun scoreCrop(profile: ColorProfile, cropRect: RectF): Float = paletteExtractor.scoreCrop(profile, cropRect)

    fun analyzeCrop(profile: ColorProfile, cropRect: RectF): CropVisualMetrics {
        return paletteExtractor.analyzeCrop(profile, cropRect)
    }
}
