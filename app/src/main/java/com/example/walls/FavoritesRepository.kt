package com.example.walls

import com.example.walls.api.WallpaperDetail


interface FavoritesRepository {
    suspend fun fetchFavoriteWallpapers(apiKey: String): List<WallpaperDetail>
    fun toggleFavorite(id: String)
    fun loadFavorites(): Set<String>
    fun isFavorite(id: String): Boolean
}