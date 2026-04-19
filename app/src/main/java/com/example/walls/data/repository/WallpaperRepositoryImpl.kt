package com.example.walls.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.walls.ThemeMode
import com.example.walls.WallpaperResponse
import com.example.walls.api.WallhavenApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WallpaperRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: WallhavenApiService
) : WallpaperRepository {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)
    }

    override suspend fun fetchWallpapers(
        apiKey: String?,
        sorting: String,
        categories: String,
        purity: String,
        page: Int
    ): WallpaperResponse {
        return apiService.searchWallpapers(apiKey, sorting, categories, purity, page)
    }

    override fun getApiKey(): String {
        return sharedPreferences.getString("API_KEY", "") ?: ""
    }

    override fun saveFilterSettings(categories: String, purity: String) {
        sharedPreferences.edit().apply {
            putString("categories", categories)
            putString("purity", purity)
            apply()
        }
    }

    override fun getFilterSettings(): Pair<String, String> {
        val categories = sharedPreferences.getString("categories", "111") ?: "111"
        val purity = sharedPreferences.getString("purity", "100") ?: "100"
        return Pair(categories, purity)
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