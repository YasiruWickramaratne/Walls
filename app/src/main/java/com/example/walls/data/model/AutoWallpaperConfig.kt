package com.example.walls.data.model

enum class RotationSource {
    FAVORITES,
    COLLECTIONS
}

enum class WallpaperScreenTarget(val persistedValue: Int) {
    HOME(0),
    LOCK(1),
    BOTH(2);

    companion object {
        fun fromPersistedValue(value: Int): WallpaperScreenTarget {
            return entries.firstOrNull { it.persistedValue == value } ?: HOME
        }
    }
}

data class AutoWallpaperConfig(
    val enabled: Boolean = false,
    val intervalMs: Long = 15 * 60 * 1000L,
    val screenTarget: WallpaperScreenTarget = WallpaperScreenTarget.HOME,
    val rotationSource: RotationSource = RotationSource.FAVORITES,
    val selectedSources: Set<String> = setOf(DEFAULT_ROTATION_COLLECTION)
) {
    companion object {
        const val DEFAULT_ROTATION_COLLECTION = "Default"
    }
}
