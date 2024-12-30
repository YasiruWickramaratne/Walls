package com.example.walls

import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val favoritesManager: FavoritesManager
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

    override suspend fun fetchFavoriteWallpapers(apiKey: String): List<FavoritesManager.WallpaperDetail> {
        return favoritesManager.fetchFavoriteWallpapers(apiKey)
    }
}