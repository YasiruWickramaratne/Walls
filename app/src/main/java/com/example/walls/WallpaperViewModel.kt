package com.example.walls

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walls.api.WallpaperDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    // Add page tracking
    private var currentRecentPage = 1
    private var currentTopPage = 1
    private var isRecentLoading = false
    private var isTopLoading = false
    private var hasRecentMorePages = true
    private var hasTopMorePages = true

    fun fetchWallpapers(sorting: String, isLoadingMore: Boolean = false) {
        val isRecent = sorting == "date_added"
        val currentPage = if (isRecent) currentRecentPage else currentTopPage
        
        if ((isRecent && isRecentLoading) || (!isRecent && isTopLoading)) return
        if ((isRecent && !hasRecentMorePages) || (!isRecent && !hasTopMorePages)) return

        viewModelScope.launch {
            try {
                if (isRecent) isRecentLoading = true else isTopLoading = true
                
                val response = wallpaperRepository.fetchWallpapers(
                    apiKey = wallpaperRepository.getApiKey(),
                    sorting = sorting,
                    categories = currentCategories,
                    purity = currentPurity,
                    page = currentPage
                )

                // Update the appropriate list
                if (isRecent) {
                    _recentWallpapers.value = if (isLoadingMore) {
                        _recentWallpapers.value + response.data
                    } else {
                        response.data
                    }
                    currentRecentPage++
                    hasRecentMorePages = response.meta.current_page < response.meta.last_page
                } else {
                    _topWallpapers.value = if (isLoadingMore) {
                        _topWallpapers.value + response.data
                    } else {
                        response.data
                    }
                    currentTopPage++
                    hasTopMorePages = response.meta.current_page < response.meta.last_page
                }
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error fetching wallpapers", e)
            } finally {
                if (isRecent) isRecentLoading = false else isTopLoading = false
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

    fun resetPagination() {
        currentRecentPage = 1
        currentTopPage = 1
        hasRecentMorePages = true
        hasTopMorePages = true
        _recentWallpapers.value = emptyList()
        _topWallpapers.value = emptyList()
    }
}

data class WallpaperResponse(
    val data: List<Wallpaper>,
    val meta: Meta
)

data class Meta(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int
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