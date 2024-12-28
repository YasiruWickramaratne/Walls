package com.example.walls

import FavoritesManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritesManager = FavoritesManager(application)

    private val _favoriteWallpapers = MutableLiveData<List<FavoritesManager.WallpaperDetail>>()
    val favoriteWallpapers: LiveData<List<FavoritesManager.WallpaperDetail>> = _favoriteWallpapers


    private val sharedPreferences: SharedPreferences by lazy {
        getApplication<Application>().getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)
    }

    private val favoritesPreferences: SharedPreferences by lazy {
        getApplication<Application>().getSharedPreferences("Favorites", Context.MODE_PRIVATE)
    }


    private val apiService: WallhavenApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://wallhaven.cc/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WallhavenApiService::class.java)
    }

    private val _favorites = MutableLiveData<Set<String>>(setOf())


    fun fetchFavoriteWallpapers() {
        viewModelScope.launch {
            val apiKey = getApiKey()
            _favoriteWallpapers.value = favoritesManager.fetchFavoriteWallpapers(apiKey)
        }
    }

    init {
        favoritesPreferences
        loadFavorites()
    }

    private val _recentWallpapers = MutableLiveData<List<Wallpaper>>()
    val recentWallpapers: LiveData<List<Wallpaper>> = _recentWallpapers

    private val _topWallpapers = MutableLiveData<List<Wallpaper>>()
    val topWallpapers: LiveData<List<Wallpaper>> = _topWallpapers

    val filterChanged = MutableLiveData<Boolean>()


    private fun getApiKey(): String {
        val apiKey = sharedPreferences.getString("API_KEY", "") ?: ""
        Log.d("WallpaperViewModel", "Fetched API key: $apiKey")
        return apiKey
    }

    fun toggleFavorite(id: String) {
        favoritesManager.toggleFavorite(id)
    }

    private fun loadFavorites() {
        _favorites.value = favoritesPreferences.getStringSet("favorite_ids", setOf()) ?: setOf()
    }

    fun isFavorite(id: String): Boolean {
        return favoritesManager.isFavorite(id)
    }

    var currentCategories: String
    var currentPurity: String

    init {
        currentCategories = sharedPreferences.getString("categories", "111") ?: "111"
        currentPurity = sharedPreferences.getString("purity", "100") ?: "100"
    }


    private var lastApiCallTime = 0L
    private val apiCallCoolDown = 5000L // 5 seconds cooldown

    fun fetchWallpapers(sorting: String) {
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastApiCallTime < apiCallCoolDown) {
                    delay(apiCallCoolDown - (currentTime - lastApiCallTime))
                }
                lastApiCallTime = System.currentTimeMillis()

                val apiKey = getApiKey()
                if (apiKey.isBlank()) {
                    Log.e("WallpaperViewModel", "API key is blank or null")
                }

                Log.d("WallpaperViewModel", "Making API call with sorting: $sorting, categories: $currentCategories, purity: $currentPurity, API Key: $apiKey")

                val response = apiService.searchWallpapers(
                    apiKey = apiKey,
                    sorting = sorting,
                    categories = currentCategories,
                    purity = currentPurity
                )
                Log.d("WallpaperViewModel", "API response received, wallpapers count: ${response.data.size}")
                when (sorting) {
                    "date_added" -> {
                        _recentWallpapers.value = response.data
                        Log.d("WallpaperViewModel", "Updated recent wallpapers, count: ${response.data.size}")
                    }
                    "toplist" -> {
                        _topWallpapers.value = response.data
                        Log.d("WallpaperViewModel", "Updated top wallpapers, count: ${response.data.size}")
                    }
                    else -> {
                        Log.w("WallpaperViewModel", "Unknown sorting type: $sorting")
                    }
                }

                filterChanged.value = false
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error fetching wallpapers", e)
                // You might want to set an error state here to show in the UI
                // For example: _errorState.value = "Failed to fetch wallpapers: ${e.message}"
            }
        }
    }

    fun updateFilters(categories: String, purity: String) {
        currentCategories = categories
        currentPurity = purity
        saveFilterSettings()
        filterChanged.value = true
    }

    private fun saveFilterSettings() {
        sharedPreferences.edit().apply {
            putString("categories", currentCategories)
            putString("purity", currentPurity)
            apply()
        }
    }

    fun isGeneralSelected() = currentCategories[0] == '1'
    fun isAnimeSelected() = currentCategories[1] == '1'
    fun isPeopleSelected() = currentCategories[2] == '1'

    fun isSfwSelected() = currentPurity[0] == '1'
    fun isSketchySelected() = currentPurity[1] == '1'
    fun isNsfwSelected() = currentPurity[2] == '1'



}

interface WallhavenApiService {
    @GET("search")
    suspend fun searchWallpapers(
        @Query("apikey") apiKey: String?,
        @Query("sorting") sorting: String,
        @Query("categories") categories: String,
        @Query("purity") purity: String
    ): WallpaperResponse
}

data class WallpaperResponse(
    val data: List<Wallpaper>
)

data class Wallpaper(
    val id: String,
    val url: String,
    val path: String,  // Add this line for the full resolution image URL
    val thumbs: Thumbs
)

data class Thumbs(
    val small: String,
    val original: String,
    val large: String
)