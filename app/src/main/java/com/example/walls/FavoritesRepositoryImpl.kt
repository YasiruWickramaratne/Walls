package com.example.walls

import com.example.walls.api.WallhavenApiService
import com.example.walls.api.WallpaperDetail
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val favoritesManager: FavoritesManager,
    private val apiService: WallhavenApiService
) : FavoritesRepository {

    override fun toggleFavorite(id: String) {
        favoritesManager.toggleFavorite(id)
    }

    override fun loadFavorites(): Set<String> {
        return favoritesManager.getFavorites()
    }

    override fun isFavorite(id: String): Boolean {
        return favoritesManager.isFavorite(id)
    }

    override suspend fun fetchFavoriteWallpapers(apiKey: String): List<WallpaperDetail> {
        val favoriteIds = favoritesManager.getFavorites()
        return favoriteIds.mapNotNull { id ->
            try {
                apiService.getWallpaperDetails(id, apiKey).data
            } catch (e: Exception) {
                null
            }
        }
    }
}