package com.example.walls

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.coroutines.delay

class WallpaperViewModel : ViewModel() {
    private val apiService: WallhavenApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://wallhaven.cc/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WallhavenApiService::class.java)
    }

    private val _recentWallpapers = MutableLiveData<List<Wallpaper>>()
    val recentWallpapers: LiveData<List<Wallpaper>> = _recentWallpapers
    val filterChanged = MutableLiveData<Boolean>()
    private val _topWallpapers = MutableLiveData<List<Wallpaper>>()
    val topWallpapers: LiveData<List<Wallpaper>> = _topWallpapers

    var currentCategories = "111"
    var currentPurity = "100"

    private var lastApiCallTime = 0L
    private val API_CALL_COOLDOWN = 5000L // 5 seconds cooldown

    fun fetchWallpapers(sorting: String, categories: String, purity: String) {
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastApiCallTime < API_CALL_COOLDOWN) {
                    delay(API_CALL_COOLDOWN - (currentTime - lastApiCallTime))
                }
                lastApiCallTime = System.currentTimeMillis()

                Log.d("WallpaperViewModel", "Making API call...")
                val response = apiService.searchWallpapers(
                    apiKey = BuildConfig.API_KEY,
                    sorting = sorting,
                    categories = categories,
                    purity = purity
                )
                if (sorting == "date_added") {
                    _recentWallpapers.value = response.data
                } else {
                    _topWallpapers.value = response.data
                }
                filterChanged.value = false
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error fetching wallpapers", e)
                // Optionally, you can set an error state here to show in the UI
            }
        }
    }

    fun updateFilters(categories: String, purity: String) {
        currentCategories = categories
        currentPurity = purity
    }
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