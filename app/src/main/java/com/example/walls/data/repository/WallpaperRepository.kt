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
        page: Int
    ): WallpaperResponse

    suspend fun fetchWallpaperDetails(
        id: String,
        apiKey: String?
    ): WallpaperDetail

    fun getApiKey(): String
    fun saveApiKey(key: String)
    fun saveFilterSettings(categories: String, purity: String)
    fun getFilterSettings(): Pair<String, String>
    fun saveThemeMode(modeName: String)
    fun getThemeMode(): com.example.walls.ThemeMode
}
