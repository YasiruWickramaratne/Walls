package com.example.walls.ui.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun WallsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var systemPaletteKey by remember { mutableStateOf(readSystemPaletteKey(context)) }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val latestPaletteKey = readSystemPaletteKey(context)
                if (latestPaletteKey != systemPaletteKey) {
                    systemPaletteKey = latestPaletteKey
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val baseColorScheme = remember(context, darkTheme, systemPaletteKey) {
        when {
            supportsDynamicColor -> {
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }
    val systemSeedColor = remember(systemPaletteKey) { extractSystemSeedColor(systemPaletteKey) }
    val colorScheme = if (supportsDynamicColor && systemSeedColor != null) {
        baseColorScheme.seededForWallP(systemSeedColor, darkTheme)
    } else if (supportsDynamicColor) {
        baseColorScheme
    } else {
        baseColorScheme.accentedForWallP(darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun readSystemPaletteKey(context: android.content.Context): String {
    return runCatching {
        Settings.Secure.getString(
            context.contentResolver,
            "theme_customization_overlay_packages"
        ).orEmpty()
    }.getOrDefault("")
}

private fun ColorScheme.accentedForWallP(darkTheme: Boolean): ColorScheme {
    val surfaceBlend = if (darkTheme) 0.18f else 0.10f
    val backgroundBlend = if (darkTheme) 0.12f else 0.07f
    val surfaceVariantBlend = if (darkTheme) 0.26f else 0.18f

    return copy(
        primary = primary,
        secondary = lerp(secondary, primary, 0.28f),
        tertiary = lerp(tertiary, primary, 0.18f),
        background = lerp(background, primaryContainer, backgroundBlend),
        surface = lerp(surface, primaryContainer, surfaceBlend),
        surfaceVariant = lerp(surfaceVariant, secondaryContainer, surfaceVariantBlend),
        surfaceContainer = lerp(surfaceContainer, primaryContainer, surfaceBlend),
        surfaceContainerHigh = lerp(surfaceContainerHigh, primaryContainer, surfaceBlend + 0.06f),
        surfaceContainerHighest = lerp(surfaceContainerHighest, primaryContainer, surfaceBlend + 0.1f),
        outline = lerp(outline, primary, 0.18f),
        outlineVariant = lerp(outlineVariant, primary, 0.12f),
        surfaceTint = primary
    )
}

private fun ColorScheme.seededForWallP(seed: Color, darkTheme: Boolean): ColorScheme {
    val primaryContainerTarget = if (darkTheme) lerp(Color.Black, seed, 0.34f) else lerp(Color.White, seed, 0.22f)
    val secondaryTarget = lerp(seed, secondary, 0.35f)
    val secondaryContainerTarget = if (darkTheme) lerp(Color.Black, seed, 0.26f) else lerp(Color.White, seed, 0.16f)
    val tertiaryTarget = lerp(seed, tertiary, 0.45f)
    val backgroundBlend = if (darkTheme) 0.10f else 0.06f
    val surfaceBlend = if (darkTheme) 0.14f else 0.08f

    return copy(
        primary = seed,
        onPrimary = seed.bestContentColor(),
        primaryContainer = primaryContainerTarget,
        onPrimaryContainer = primaryContainerTarget.bestContentColor(),
        secondary = secondaryTarget,
        onSecondary = secondaryTarget.bestContentColor(),
        secondaryContainer = secondaryContainerTarget,
        onSecondaryContainer = secondaryContainerTarget.bestContentColor(),
        tertiary = tertiaryTarget,
        onTertiary = tertiaryTarget.bestContentColor(),
        background = lerp(background, seed, backgroundBlend),
        surface = lerp(surface, seed, surfaceBlend),
        surfaceContainer = lerp(surfaceContainer, seed, surfaceBlend),
        surfaceContainerHigh = lerp(surfaceContainerHigh, seed, surfaceBlend + 0.04f),
        surfaceContainerHighest = lerp(surfaceContainerHighest, seed, surfaceBlend + 0.08f),
        surfaceTint = seed,
        outline = lerp(outline, seed, 0.18f),
        outlineVariant = lerp(outlineVariant, seed, 0.12f)
    )
}

private fun extractSystemSeedColor(systemPaletteKey: String): Color? {
    val match = Regex("\"android\\.theme\\.customization\\.system_palette\":\"(ff[0-9a-fA-F]{6})\"")
        .find(systemPaletteKey)
        ?: Regex("\"android\\.theme\\.customization\\.accent_color\":\"(ff[0-9a-fA-F]{6})\"")
            .find(systemPaletteKey)
    val hex = match?.groupValues?.getOrNull(1) ?: return null
    return runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrNull()
}

private fun Color.bestContentColor(): Color {
    return if (luminance() > 0.5f) Color(0xFF111111) else Color.White
}
