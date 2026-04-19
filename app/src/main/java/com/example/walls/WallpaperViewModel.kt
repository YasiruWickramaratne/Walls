package com.example.walls

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walls.api.WallpaperDetail
import com.example.walls.data.repository.FavoritesRepository
import com.example.walls.data.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _favoriteWallpapers = MutableStateFlow<List<WallpaperDetail>>(emptyList())
    val favoriteWallpapers: StateFlow<List<WallpaperDetail>> = _favoriteWallpapers

    private val _favorites = MutableStateFlow<Set<String>>(setOf())
    val favorites: StateFlow<Set<String>> = _favorites

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        wallpaperRepository.saveThemeMode(mode.name)
    }

    fun refreshThemeMode() {
        _themeMode.value = wallpaperRepository.getThemeMode()
    }

    fun saveApiKey(key: String) {
        wallpaperRepository.saveApiKey(key)
    }

    init {
        loadFavorites()
        _themeMode.value = wallpaperRepository.getThemeMode()
    }

    fun fetchFavoriteWallpapers() {
        viewModelScope.launch {
            val apiKey = wallpaperRepository.getApiKey()
            try {
                val favorites = favoritesRepository.fetchFavoriteWallpapers(apiKey)
                _favoriteWallpapers.value = favorites
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error fetching favorite wallpapers", e)
            }
        }
    }

    private val _recentWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val recentWallpapers: StateFlow<List<Wallpaper>> = _recentWallpapers

    private val _topWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val topWallpapers: StateFlow<List<Wallpaper>> = _topWallpapers

    private val _filterChanged = MutableStateFlow(false)
    val filterChanged: StateFlow<Boolean> = _filterChanged

    fun loadFavorites() {
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

    // Separate loading states for each tab
    private var isRecentLoading = false
    private var isTopLoading = false
    private var hasRecentMorePages = true
    private var hasTopMorePages = true
    private var currentRecentPage = 1
    private var currentTopPage = 1

    // Cache the responses
    private var cachedRecentWallpapers = mutableListOf<Wallpaper>()
    private var cachedTopWallpapers = mutableListOf<Wallpaper>()
    
    private var isReturningFromFullScreen = false

    fun fetchWallpapers(sorting: String, isLoadingMore: Boolean = false) {
        val isRecent = sorting == "date_added"
        
        // Only skip if returning from FullScreen AND not applying filters
        if (isReturningFromFullScreen && !_filterChanged.value) {
            isReturningFromFullScreen = false
            return
        }

        // Reset the filter changed flag after starting the fetch
        if (_filterChanged.value) {
            _filterChanged.value = false
        }

        // Don't fetch if we're just returning from FullScreenImageActivity
        if (isReturningFromFullScreen) {
            isReturningFromFullScreen = false
            return
        }

        // Don't fetch if already loading or no more pages
        if ((isRecent && isRecentLoading) || (!isRecent && isTopLoading)) return
        if ((isRecent && !hasRecentMorePages) || (!isRecent && !hasTopMorePages)) return

        // If not loading more and we have cached data, use it
        if (!isLoadingMore) {
            if (isRecent && cachedRecentWallpapers.isNotEmpty()) {
                _recentWallpapers.value = cachedRecentWallpapers
                return
            } else if (!isRecent && cachedTopWallpapers.isNotEmpty()) {
                _topWallpapers.value = cachedTopWallpapers
                return
            }
        }

        viewModelScope.launch {
            try {
                if (isRecent) isRecentLoading = true else isTopLoading = true
                
                val currentPage = if (isRecent) currentRecentPage else currentTopPage
                
                val response = wallpaperRepository.fetchWallpapers(
                    apiKey = wallpaperRepository.getApiKey(),
                    sorting = sorting,
                    categories = currentCategories,
                    purity = currentPurity,
                    page = currentPage
                )

                // Update the appropriate list and cache
                if (isRecent) {
                    if (!isLoadingMore) {
                        cachedRecentWallpapers.clear()
                    }
                    cachedRecentWallpapers.addAll(response.data)
                    _recentWallpapers.value = cachedRecentWallpapers.toList()
                    currentRecentPage++
                    hasRecentMorePages = response.meta.current_page < response.meta.last_page
                } else {
                    if (!isLoadingMore) {
                        cachedTopWallpapers.clear()
                    }
                    cachedTopWallpapers.addAll(response.data)
                    _topWallpapers.value = cachedTopWallpapers.toList()
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

            if (isNsfwSelected()) {
                val apiKey = wallpaperRepository.getApiKey()
                if (apiKey.isEmpty()) {
                    //Log.e("WallpaperViewModel", "API key is required for NSFW content")
                    _errorMessage.emit("API key is required for NSFW content!")
                    // For example, you could use a LiveData to communicate with the UI
                    // _errorMessage.value = "API key is required for NSFW content"
                    return@launch
                }
                //Log.d("WallpaperViewModel", "API key verified for NSFW content")
            }

            wallpaperRepository.saveFilterSettings(categories, purity)

            // Clear caches and reset pagination
            resetPagination()
            
            // Set filter changed flag
            _filterChanged.value = true
            
            // Fetch both types of wallpapers with new filters
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
        cachedRecentWallpapers.clear()
        cachedTopWallpapers.clear()
        _recentWallpapers.value = emptyList()
        _topWallpapers.value = emptyList()
    }

    fun setReturningFromFullScreen() {
        isReturningFromFullScreen = true
    }

    fun getNextPageForSorting(sorting: String): Int {
        return if (sorting == "date_added") currentRecentPage else currentTopPage
    }

    fun hasMorePagesForSorting(sorting: String): Boolean {
        return if (sorting == "date_added") hasRecentMorePages else hasTopMorePages
    }

    fun seedWallpapersForSorting(
        sorting: String,
        wallpapers: List<Wallpaper>,
        nextPage: Int,
        hasMorePages: Boolean
    ) {
        if (sorting == "date_added") {
            cachedRecentWallpapers.clear()
            cachedRecentWallpapers.addAll(wallpapers)
            _recentWallpapers.value = wallpapers
            currentRecentPage = nextPage
            hasRecentMorePages = hasMorePages
        } else {
            cachedTopWallpapers.clear()
            cachedTopWallpapers.addAll(wallpapers)
            _topWallpapers.value = wallpapers
            currentTopPage = nextPage
            hasTopMorePages = hasMorePages
        }
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
