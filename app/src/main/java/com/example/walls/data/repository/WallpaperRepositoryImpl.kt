package com.example.walls.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.walls.ThemeMode
import com.example.walls.WallpaperResponse
import com.example.walls.api.WallhavenApiService
import com.example.walls.api.WallpaperDetail
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WallpaperRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiService: WallhavenApiService
) : WallpaperRepository {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)
    }

    override suspend fun fetchWallpapers(
        apiKey: String?,
        query: String?,
        sorting: String,
        categories: String,
        purity: String,
        resolutions: String?,
        ratios: String?,
        colors: String?,
        page: Int
    ): WallpaperResponse {
        return apiService.searchWallpapers(
            apiKey = apiKey,
            query = query,
            sorting = sorting,
            categories = categories,
            purity = purity,
            resolutions = resolutions,
            ratios = ratios,
            colors = colors,
            page = page
        )
    }

    override suspend fun fetchWallpaperDetails(id: String, apiKey: String?): WallpaperDetail {
        return apiService.getWallpaperDetails(id, apiKey?.takeIf { it.isNotBlank() }).data
    }

    override fun getApiKey(): String {
        return sharedPreferences.getString("API_KEY", "") ?: ""
    }

    override fun saveFilterSettings(
        categories: String,
        purity: String,
        resolution: String,
        ratio: String,
        color: String
    ) {
        sharedPreferences.edit().apply {
            putString("categories", categories)
            putString("purity", purity)
            putString("resolution", resolution)
            putString("ratio", ratio)
            putString("color", color)
            apply()
        }
    }

    override fun getFilterSettings(): SavedFilterSettings {
        val categories = sharedPreferences.getString("categories", "111") ?: "111"
        val purity = sharedPreferences.getString("purity", "100") ?: "100"
        val resolution = sharedPreferences.getString("resolution", "") ?: ""
        val ratio = sharedPreferences.getString("ratio", "") ?: ""
        val color = sharedPreferences.getString("color", "") ?: ""
        return SavedFilterSettings(
            categories = categories,
            purity = purity,
            resolution = resolution,
            ratio = ratio,
            color = color
        )
    }

    override fun saveApiKey(key: String) {
        sharedPreferences.edit().putString("API_KEY", key).apply()
    }

    override fun saveThemeMode(modeName: String) {
        sharedPreferences.edit().putString("THEME_MODE", modeName).apply()
    }

    override fun getThemeMode(): ThemeMode {
        val name = sharedPreferences.getString("THEME_MODE", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return ThemeMode.valueOf(name)
    }
}
