package com.example.walls

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walls.api.WallpaperDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _favoriteWallpapers = MutableStateFlow<List<WallpaperDetail>>(emptyList())
    val favoriteWallpapers: StateFlow<List<WallpaperDetail>> = _favoriteWallpapers

    private val _favorites = MutableStateFlow<Set<String>>(setOf())
    val favorites: StateFlow<Set<String>> = _favorites

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
        viewModelScope.launch {
            _favorites.value = favoritesRepository.loadFavorites()
        }
    }

    fun isFavorite(id: String): Boolean {
        return _favorites.value.contains(id)
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(id)
            // Update the favorites state after toggling
            _favorites.value = favoritesRepository.loadFavorites()
            Log.d("WallpaperViewModel", "Favorite toggled for $id, new favorites: ${_favorites.value}")
        }
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
                Log.d("WallpaperViewModel", "Fetching wallpapers for sorting: $sorting")
                
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastApiCallTime < apiCallCoolDown) {
                    delay(apiCallCoolDown - (currentTime - lastApiCallTime))
                }
                lastApiCallTime = System.currentTimeMillis()

                val apiKey = wallpaperRepository.getApiKey()
                
                val wallpapers = wallpaperRepository.fetchWallpapers(
                    apiKey = apiKey,
                    sorting = sorting,
                    categories = currentCategories,
                    purity = currentPurity
                )

                Log.d("WallpaperViewModel", "Received ${wallpapers.size} wallpapers for $sorting")

                when (sorting) {
                    "date_added" -> {
                        _recentWallpapers.value = wallpapers
                        Log.d("WallpaperViewModel", "Updated recent wallpapers")
                    }
                    "toplist" -> {
                        _topWallpapers.value = wallpapers
                        Log.d("WallpaperViewModel", "Updated top wallpapers")
                    }
                }

                // Only set filterChanged to false after both fetches are complete
                if (sorting == "toplist") {
                    _filterChanged.value = false
                }
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error fetching wallpapers", e)
            }
        }
    }

    fun updateFilters(categories: String, purity: String) {
        viewModelScope.launch {
            Log.d("WallpaperViewModel", "Updating filters: categories=$categories, purity=$purity")
            currentCategories = categories
            currentPurity = purity
            wallpaperRepository.saveFilterSettings(categories, purity)
            
            // Set filter changed before fetching
            _filterChanged.value = true
            
            // Fetch both types of wallpapers
            Log.d("WallpaperViewModel", "Fetching wallpapers with new filters")
            fetchWallpapers("date_added")
            fetchWallpapers("toplist")
        }
    }

    fun isGeneralSelected() = currentCategories[0] == '1'
    fun isAnimeSelected() = currentCategories[1] == '1'
    fun isPeopleSelected() = currentCategories[2] == '1'

    fun isSfwSelected() = currentPurity[0] == '1'
    fun isSketchySelected() = currentPurity[1] == '1'
    fun isNsfwSelected() = currentPurity[2] == '1'
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