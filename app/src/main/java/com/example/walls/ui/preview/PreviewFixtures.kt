package com.example.walls.ui.preview

import com.example.walls.Thumbs
import com.example.walls.Wallpaper
import com.example.walls.api.WallpaperDetail
import com.example.walls.api.WallpaperTag
import com.example.walls.data.repository.FavoriteCollection

internal fun previewWallpaper(id: String = "preview-$idSeed"): Wallpaper {
    return Wallpaper(
        id = id,
        url = "https://wallhaven.cc/w/$id",
        path = "",
        thumbs = Thumbs(
            small = "",
            original = "",
            large = ""
        )
    )
}

internal fun previewWallpapers(count: Int = 8): List<Wallpaper> {
    return List(count) { index -> previewWallpaper("preview-${index + 1}") }
}

internal fun previewCollections(): List<FavoriteCollection> {
    return listOf(
        FavoriteCollection("Nature", setOf("preview-1", "preview-2")),
        FavoriteCollection("Architecture", setOf("preview-3")),
        FavoriteCollection("Dark", emptySet())
    )
}

internal fun previewWallpaperDetail(): WallpaperDetail {
    return WallpaperDetail(
        id = "preview-1",
        url = "https://wallhaven.cc/w/preview-1",
        path = "",
        views = 42100,
        favorites = 2184,
        category = "general",
        purity = "sfw",
        dimension_x = 3840,
        dimension_y = 2160,
        resolution = "3840x2160",
        file_size = 4_620_000,
        file_type = "image/jpeg",
        created_at = "2026-05-17",
        colors = listOf("#4A5C58", "#E7D28B", "#2F3337", "#A15E3A", "#C7D3D5"),
        tags = listOf(
            WallpaperTag(1, "castle"),
            WallpaperTag(2, "landscape"),
            WallpaperTag(3, "flowers")
        ),
        thumbs = com.example.walls.api.Thumbs(
            large = "",
            original = "",
            small = ""
        )
    )
}

private const val idSeed = "sample"
