package com.example.walls

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walls.api.WallpaperDetail
import com.example.walls.data.manager.FavoritesCollectionManager
import com.example.walls.data.repository.FavoriteCollection
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
    private val favoritesCollectionManager: FavoritesCollectionManager
) : ViewModel() {

    private val _favoriteWallpapers = MutableStateFlow<List<WallpaperDetail>>(emptyList())
    val favoriteWallpapers: StateFlow<List<WallpaperDetail>> = _favoriteWallpapers

    private val _favorites = MutableStateFlow<Set<String>>(setOf())
    val favorites: StateFlow<Set<String>> = _favorites

    private val _favoriteCollections = MutableStateFlow<List<FavoriteCollection>>(emptyList())
    val favoriteCollections: StateFlow<List<FavoriteCollection>> = _favoriteCollections

    private val _selectedFavoritesCollection = MutableStateFlow<String?>(null)
    val selectedFavoritesCollection: StateFlow<String?> = _selectedFavoritesCollection

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _wallpaperDetails = MutableStateFlow<Map<String, WallpaperDetail>>(emptyMap())
    val wallpaperDetails: StateFlow<Map<String, WallpaperDetail>> = _wallpaperDetails

    private val _wallpaperDetailsLoading = MutableStateFlow<Set<String>>(emptySet())
    val wallpaperDetailsLoading: StateFlow<Set<String>> = _wallpaperDetailsLoading

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

    fun getApiKey(): String = wallpaperRepository.getApiKey()

    init {
        loadFavorites()
        _themeMode.value = wallpaperRepository.getThemeMode()
    }

    fun fetchFavoriteWallpapers() {
        viewModelScope.launch {
            val apiKey = wallpaperRepository.getApiKey()
            try {
                val favorites = favoritesCollectionManager.fetchFavoriteWallpapers(
                    apiKey = apiKey,
                    collectionName = _selectedFavoritesCollection.value
                )
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

    private val _searchWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val searchWallpapers: StateFlow<List<Wallpaper>> = _searchWallpapers

    private val _currentSearchQuery = MutableStateFlow("")
    val currentSearchQuery: StateFlow<String> = _currentSearchQuery

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading

    private val _hasCompletedSearch = MutableStateFlow(false)
    val hasCompletedSearch: StateFlow<Boolean> = _hasCompletedSearch

    private val _filterChanged = MutableStateFlow(false)
    val filterChanged: StateFlow<Boolean> = _filterChanged

    fun loadFavorites() {
        viewModelScope.launch {
            refreshFavoritesState()
        }
    }

    fun isFavorite(id: String): Boolean {
        return _favorites.value.contains(id)
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            favoritesCollectionManager.toggleFavorite(id)
            refreshFavoritesState()
            Log.d("WallpaperViewModel", "Favorite toggled for $id, new favorites: ${_favorites.value}")
        }
    }

    fun addFavorite(id: String) {
        viewModelScope.launch {
            val added = favoritesCollectionManager.addFavorite(id)
            if (added) {
                refreshFavoritesState()
            }
        }
    }

    fun selectFavoritesCollection(name: String?) {
        _selectedFavoritesCollection.value = name
        fetchFavoriteWallpapers()
    }

    fun createFavoriteCollection(name: String): Boolean {
        val created = favoritesCollectionManager.createCollection(name)
        _favoriteCollections.value = favoritesCollectionManager.getCollections()
        return created
    }

    fun deleteFavoriteCollection(name: String): Boolean {
        val deleted = favoritesCollectionManager.deleteCollection(name)
        _favoriteCollections.value = favoritesCollectionManager.getCollections()
        if (_selectedFavoritesCollection.value == name) {
            _selectedFavoritesCollection.value = null
        }
        return deleted
    }

    fun addWallpaperToCollection(collectionName: String, wallpaperId: String) {
        favoritesCollectionManager.addToCollection(collectionName, wallpaperId)
        _favoriteCollections.value = favoritesCollectionManager.getCollections()
    }

    fun toggleWallpaperInCollection(collectionName: String, wallpaperId: String): Boolean {
        val isNowIncluded = favoritesCollectionManager.toggleCollectionMembership(collectionName, wallpaperId)
        _favoriteCollections.value = favoritesCollectionManager.getCollections()
        return isNowIncluded
    }

    private fun refreshFavoritesState() {
        _favorites.value = favoritesCollectionManager.loadFavorites()
        _favoriteCollections.value = favoritesCollectionManager.getCollections()
    }

    var currentCategories: String
    var currentPurity: String
    var currentResolution: String
    var currentRatio: String
    var currentColor: String
    var searchCategories: String = "111"
    var searchPurity: String = "100"
    var searchResolution: String = ""
    var searchRatio: String = ""
    var searchColor: String = ""

    init {
        val filters = wallpaperRepository.getFilterSettings()
        currentCategories = filters.categories
        currentPurity = filters.purity
        currentResolution = filters.resolution
        currentRatio = filters.ratio
        currentColor = filters.color
    }

    // Separate loading states for each tab
    private var isRecentLoading = false
    private var isTopLoading = false
    private var hasRecentMorePages = true
    private var hasTopMorePages = true
    private var hasSearchMorePages = true
    private var currentRecentPage = 1
    private var currentTopPage = 1
    private var currentSearchPage = 1

    // Cache the responses
    private var cachedRecentWallpapers = mutableListOf<Wallpaper>()
    private var cachedTopWallpapers = mutableListOf<Wallpaper>()
    private var cachedSearchWallpapers = mutableListOf<Wallpaper>()
    private var isSearchLoading = false
    private var isReturningFromFullScreen = false
    private var lastFetchedSearchQuery = ""

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
                    query = null,
                    sorting = sorting,
                    categories = currentCategories,
                    purity = currentPurity,
                    resolutions = currentResolution.ifBlank { null },
                    ratios = currentRatio.ifBlank { null },
                    colors = currentColor.ifBlank { null },
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

    fun updateFilters(
        categories: String,
        purity: String,
        resolution: String,
        ratio: String,
        color: String
    ) {



        viewModelScope.launch {
            Log.d("WallpaperViewModel", "Updating filters: categories=$categories, purity=$purity")
            currentCategories = categories
            currentPurity = purity
            currentResolution = resolution
            currentRatio = ratio
            currentColor = color

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

            wallpaperRepository.saveFilterSettings(categories, purity, resolution, ratio, color)

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
    fun isSearchGeneralSelected() = searchCategories[0] == '1'
    fun isSearchAnimeSelected() = searchCategories[1] == '1'
    fun isSearchPeopleSelected() = searchCategories[2] == '1'
    fun isSearchSfwSelected() = searchPurity[0] == '1'
    fun isSearchSketchySelected() = searchPurity[1] == '1'
    fun isSearchNsfwSelected() = searchPurity[2] == '1'

    fun updateSearchFilters(
        categories: String,
        purity: String,
        resolution: String,
        ratio: String,
        color: String
    ) {
        searchCategories = categories
        searchPurity = purity
        searchResolution = resolution
        searchRatio = ratio
        searchColor = color
        currentSearchPage = 1
        hasSearchMorePages = true
        lastFetchedSearchQuery = ""
        _searchLoading.value = false
        _hasCompletedSearch.value = false
        cachedSearchWallpapers.clear()
        _searchWallpapers.value = emptyList()
    }

    fun resetSearchFilters(clearResults: Boolean = false) {
        searchCategories = "111"
        searchPurity = "100"
        searchResolution = ""
        searchRatio = ""
        searchColor = ""
        if (clearResults) {
            currentSearchPage = 1
            hasSearchMorePages = true
            lastFetchedSearchQuery = ""
            _searchLoading.value = false
            _hasCompletedSearch.value = false
            _currentSearchQuery.value = ""
            cachedSearchWallpapers.clear()
            _searchWallpapers.value = emptyList()
        }
    }

    fun resetPagination() {
        currentRecentPage = 1
        currentTopPage = 1
        currentSearchPage = 1
        hasRecentMorePages = true
        hasTopMorePages = true
        hasSearchMorePages = true
        lastFetchedSearchQuery = ""
        _searchLoading.value = false
        _hasCompletedSearch.value = false
        cachedRecentWallpapers.clear()
        cachedTopWallpapers.clear()
        cachedSearchWallpapers.clear()
        _recentWallpapers.value = emptyList()
        _topWallpapers.value = emptyList()
        _searchWallpapers.value = emptyList()
    }

    fun setReturningFromFullScreen() {
        isReturningFromFullScreen = true
    }

    fun fetchWallpaperDetails(id: String) {
        if (_wallpaperDetails.value.containsKey(id) || _wallpaperDetailsLoading.value.contains(id)) return

        viewModelScope.launch {
            _wallpaperDetailsLoading.value = _wallpaperDetailsLoading.value + id
            try {
                val detail = wallpaperRepository.fetchWallpaperDetails(
                    id = id,
                    apiKey = wallpaperRepository.getApiKey().ifBlank { null }
                )
                _wallpaperDetails.value = _wallpaperDetails.value + (id to detail)
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error fetching wallpaper details for $id", e)
            } finally {
                _wallpaperDetailsLoading.value = _wallpaperDetailsLoading.value - id
            }
        }
    }

    fun getNextPageForSorting(sorting: String): Int {
        return when (sorting) {
            "date_added" -> currentRecentPage
            "toplist" -> currentTopPage
            "search" -> currentSearchPage
            else -> currentRecentPage
        }
    }

    fun hasMorePagesForSorting(sorting: String): Boolean {
        return when (sorting) {
            "date_added" -> hasRecentMorePages
            "toplist" -> hasTopMorePages
            "search" -> hasSearchMorePages
            else -> hasRecentMorePages
        }
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
        } else if (sorting == "toplist") {
            cachedTopWallpapers.clear()
            cachedTopWallpapers.addAll(wallpapers)
            _topWallpapers.value = wallpapers
            currentTopPage = nextPage
            hasTopMorePages = hasMorePages
        } else if (sorting == "search") {
            cachedSearchWallpapers.clear()
            cachedSearchWallpapers.addAll(wallpapers)
            _searchWallpapers.value = wallpapers
            currentSearchPage = nextPage
            hasSearchMorePages = hasMorePages
        }
    }

    fun setCurrentSearchQuery(query: String) {
        _currentSearchQuery.value = query.trim()
    }

    fun getCurrentSearchQuery(): String = _currentSearchQuery.value

    fun fetchSearchWallpapers(query: String, isLoadingMore: Boolean = false) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            _currentSearchQuery.value = ""
            currentSearchPage = 1
            hasSearchMorePages = true
            lastFetchedSearchQuery = ""
            _searchLoading.value = false
            _hasCompletedSearch.value = false
            cachedSearchWallpapers.clear()
            _searchWallpapers.value = emptyList()
            return
        }

        val isNewQuery = normalizedQuery != lastFetchedSearchQuery
        if (isNewQuery) {
            _currentSearchQuery.value = normalizedQuery
            currentSearchPage = 1
            hasSearchMorePages = true
            cachedSearchWallpapers.clear()
            _searchWallpapers.value = emptyList()
        }

        if (isSearchLoading || !hasSearchMorePages) return

        if (!isLoadingMore && !isNewQuery && cachedSearchWallpapers.isNotEmpty()) {
            _searchWallpapers.value = cachedSearchWallpapers.toList()
            _hasCompletedSearch.value = true
            return
        }

        if (!isLoadingMore) {
            _searchLoading.value = true
            _hasCompletedSearch.value = false
        }

        viewModelScope.launch {
            try {
                isSearchLoading = true
                val response = wallpaperRepository.fetchWallpapers(
                    apiKey = wallpaperRepository.getApiKey(),
                    query = normalizedQuery,
                    sorting = "relevance",
                    categories = searchCategories,
                    purity = searchPurity,
                    resolutions = searchResolution.ifBlank { null },
                    ratios = searchRatio.ifBlank { null },
                    colors = searchColor.ifBlank { null },
                    page = currentSearchPage
                )
                if (!isLoadingMore || isNewQuery) {
                    cachedSearchWallpapers.clear()
                }
                lastFetchedSearchQuery = normalizedQuery
                cachedSearchWallpapers.addAll(response.data)
                _searchWallpapers.value = cachedSearchWallpapers.toList()
                currentSearchPage++
                hasSearchMorePages = response.meta.current_page < response.meta.last_page
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error fetching search wallpapers", e)
            } finally {
                isSearchLoading = false
                if (!isLoadingMore) {
                    _searchLoading.value = false
                    _hasCompletedSearch.value = true
                }
            }
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
