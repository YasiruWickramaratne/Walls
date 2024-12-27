package com.example.walls

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.coroutines.delay

class WallpaperViewModel(context: Context) : ViewModel() {
    private val apiService: WallhavenApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://wallhaven.cc/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WallhavenApiService::class.java)
    }


    private val _recentWallpapers = MutableLiveData<List<Wallpaper>>()
    val recentWallpapers: LiveData<List<Wallpaper>> = _recentWallpapers

    private val _topWallpapers = MutableLiveData<List<Wallpaper>>()
    val topWallpapers: LiveData<List<Wallpaper>> = _topWallpapers

    val filterChanged = MutableLiveData<Boolean>()

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
    }

    var currentCategories: String
    var currentPurity: String

    init {
        currentCategories = sharedPreferences.getString("categories", "111") ?: "111"
        currentPurity = sharedPreferences.getString("purity", "100") ?: "100"
    }


    private var lastApiCallTime = 0L
    private val API_CALL_COOLDOWN = 5000L // 5 seconds cooldown

    fun fetchWallpapers(sorting: String) {
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastApiCallTime < API_CALL_COOLDOWN) {
                    delay(API_CALL_COOLDOWN - (currentTime - lastApiCallTime))
                }
                lastApiCallTime = System.currentTimeMillis()

                Log.d("WallpaperViewModel", "Making API call with sorting: $sorting, categories: $currentCategories, purity: $currentPurity")
                val response = apiService.searchWallpapers(
                    apiKey = BuildConfig.API_KEY,
                    sorting = sorting,
                    categories = currentCategories,
                    purity = currentPurity
                )

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

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WallpaperViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WallpaperViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
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
        @Query("apikey") apiKey: String,
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
    val thumbs: Thumbs
)

data class Thumbs(
    val small: String,
    val original: String,
    val large: String
)