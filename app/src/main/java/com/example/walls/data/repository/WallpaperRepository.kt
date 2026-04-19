package com.example.walls.data.repository

import com.example.walls.api.WallpaperDetail
import com.example.walls.WallpaperResponse

interface WallpaperRepository {
    suspend fun fetchWallpapers(
        apiKey: String?,
        query: String?,
        sorting: String,
        categories: String,
        purity: String,
        resolutions: String?,
        ratios: String?,
        colors: String?,
        page: Int
    ): WallpaperResponse

    suspend fun fetchWallpaperDetails(
        id: String,
        apiKey: String?
    ): WallpaperDetail

    fun getApiKey(): String
    fun saveApiKey(key: String)
    fun saveFilterSettings(
        categories: String,
        purity: String,
        resolution: String,
        ratio: String,
        color: String
    )
    fun getFilterSettings(): SavedFilterSettings
    fun saveThemeMode(modeName: String)
    fun getThemeMode(): com.example.walls.ThemeMode
}

data class SavedFilterSettings(
    val categories: String,
    val purity: String,
    val resolution: String,
    val ratio: String,
    val color: String
)
