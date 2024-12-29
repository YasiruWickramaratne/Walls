package com.example.walls

import FavoritesManager

interface FavoritesRepository {
    suspend fun fetchFavoriteWallpapers(apiKey: String): List<FavoritesManager.WallpaperDetail>
    fun toggleFavorite(id: String)
    fun loadFavorites(): Set<String>
    fun isFavorite(id: String): Boolean
}