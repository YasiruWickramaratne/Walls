package com.example.walls.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Amber80 = Color(0xFFF5DFA1)
val AmberGrey80 = Color(0xFFD4C3A5)
val Coral80 = Color(0xFFF3C5A8)

val Amber40 = Color(0xFF8B5E18)
val AmberGrey40 = Color(0xFF75624A)
val Coral40 = Color(0xFF8C5A3C)

val LightColorScheme = lightColorScheme(
    primary = Amber40,
    secondary = AmberGrey40,
    tertiary = Coral40,
    background = Color(0xFFFFF8F1),
    surface = Color(0xFFFFF8F1),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

val DarkColorScheme = darkColorScheme(
    primary = Amber80,
    secondary = AmberGrey80,
    tertiary = Coral80,
    background = Color(0xFF17120D),
    surface = Color(0xFF17120D),
    onPrimary = Color(0xFF4D2F00),
    onSecondary = Color(0xFF433424),
    onTertiary = Color(0xFF4A2E1B),
    onBackground = Color(0xFFF0E1D2),
    onSurface = Color(0xFFF0E1D2),
)
