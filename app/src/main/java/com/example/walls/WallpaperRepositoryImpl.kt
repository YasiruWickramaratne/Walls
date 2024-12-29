package com.example.walls

import android.content.Context
import android.content.SharedPreferences
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WallpaperRepositoryImpl(private val context: Context) : WallpaperRepository {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)
    }

    private val apiService: WallhavenApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://wallhaven.cc/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WallhavenApiService::class.java)
    }

    override suspend fun fetchWallpapers(
        apiKey: String?,
        sorting: String,
        categories: String,
        purity: String
    ): List<Wallpaper> {
        val response = apiService.searchWallpapers(apiKey, sorting, categories, purity)
        return response.data
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
}