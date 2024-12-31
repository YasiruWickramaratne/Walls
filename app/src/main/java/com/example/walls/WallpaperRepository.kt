package com.example.walls

interface WallpaperRepository {
    suspend fun fetchWallpapers(
        apiKey: String?,
        sorting: String,
        categories: String,
        purity: String,
        page: Int
    ): WallpaperResponse

    fun getApiKey(): String
    fun saveFilterSettings(categories: String, purity: String)
    fun getFilterSettings(): Pair<String, String>
}