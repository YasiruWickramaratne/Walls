package com.example.walls


import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.http.GET
import retrofit2.http.Query

class WallpaperViewModel(
    application: Application,
    private val wallpaperRepository: WallpaperRepository,
    private val favoritesRepository: FavoritesRepository
) : AndroidViewModel(application) {

    private val _favoriteWallpapers = MutableStateFlow<List<FavoritesManager.WallpaperDetail>>(emptyList())
    val favoriteWallpapers: StateFlow<List<FavoritesManager.WallpaperDetail>> = _favoriteWallpapers

    private val _favorites = MutableStateFlow<Set<String>>(setOf())

    init {
        loadFavorites()
    }

    fun fetchFavoriteWallpapers() {
        viewModelScope.launch {
            val apiKey = wallpaperRepository.getApiKey()
            _favoriteWallpapers.value = favoritesRepository.fetchFavoriteWallpapers(apiKey)
        }
    }

    private val _recentWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val recentWallpapers: StateFlow<List<Wallpaper>> = _recentWallpapers

    private val _topWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val topWallpapers: StateFlow<List<Wallpaper>> = _topWallpapers

    private val _filterChanged = MutableStateFlow(false)
    val filterChanged: StateFlow<Boolean> = _filterChanged

    private fun loadFavorites() {
        _favorites.value = favoritesRepository.loadFavorites()
    }

    fun toggleFavorite(id: String) {
        favoritesRepository.toggleFavorite(id)
    }

    fun isFavorite(id: String): Boolean {
        return favoritesRepository.isFavorite(id)
    }

    var currentCategories: String
    var currentPurity: String

    init {
        val (categories, purity) = wallpaperRepository.getFilterSettings()
        currentCategories = categories
        currentPurity = purity
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

                val apiKey = wallpaperRepository.getApiKey()
                if (apiKey.isBlank()) {
                    Log.e("WallpaperViewModel", "API key is blank or null")
                }

                Log.d(
                    "WallpaperViewModel",
                    "Making API call with sorting: $sorting, categories: $currentCategories, purity: $currentPurity, API Key: $apiKey"
                )

                val wallpapers = wallpaperRepository.fetchWallpapers(
                    apiKey = apiKey,
                    sorting = sorting,
                    categories = currentCategories,
                    purity = currentPurity
                )

                when (sorting) {
                    "date_added" -> {
                        _recentWallpapers.value = wallpapers
                        Log.d(
                            "WallpaperViewModel",
                            "Updated recent wallpapers, count: ${wallpapers.size}"
                        )
                    }

                    "toplist" -> {
                        _topWallpapers.value = wallpapers
                        Log.d(
                            "WallpaperViewModel",
                            "Updated top wallpapers, count: ${wallpapers.size}"
                        )
                    }

                    else -> {
                        Log.w("WallpaperViewModel", "Unknown sorting type: $sorting")
                    }
                }

                _filterChanged.value = false
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error fetching wallpapers", e)
            }
        }
    }

    fun updateFilters(categories: String, purity: String) {
        currentCategories = categories
        currentPurity = purity
        wallpaperRepository.saveFilterSettings(categories, purity)
        _filterChanged.value = true
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