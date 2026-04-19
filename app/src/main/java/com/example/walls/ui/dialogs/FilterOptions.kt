package com.example.walls.ui.dialogs

val resolutionOptions = listOf(
    "1920x1080",
    "2560x1440",
    "3440x1440",
    "3840x2160",
    "5120x2880",
    "7680x4320",
    "1080x1920",
    "1440x2560"
)

val wideResolutionOptions = resolutionOptions.filter { resolutionOrientation(it) == "Wide" }
val portraitResolutionOptions = resolutionOptions.filter { resolutionOrientation(it) == "Portrait" }
val squareResolutionOptions = resolutionOptions.filter { resolutionOrientation(it) == "Square" }

val ratioOptions = listOf("Any", "16x9", "16x10", "21x9", "4x3", "5x4", "32x9", "9x16")

val colorOptions = listOf(
    "660000", "cc3333", "ea4c88", "993399", "333399", "0066cc",
    "0099cc", "66cccc", "77cc33", "669900", "cccc33", "ffcc33",
    "ff9900", "ff6600", "996633", "000000", "999999", "cccccc", "ffffff", "424153"
)

fun resolutionOrientation(resolution: String): String {
    val parts = resolution.split("x")
    if (parts.size != 2) return "Wide"
    val width = parts[0].toIntOrNull() ?: return "Wide"
    val height = parts[1].toIntOrNull() ?: return "Wide"
    return when {
        width > height -> "Wide"
        width < height -> "Portrait"
        else -> "Square"
    }
}
