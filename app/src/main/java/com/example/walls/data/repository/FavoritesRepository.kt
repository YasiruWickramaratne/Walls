package com.example.walls.data.repository

import com.example.walls.api.WallpaperDetail

data class FavoriteCollection(
    val name: String,
    val wallpaperIds: Set<String>
)

interface FavoritesRepository {
    suspend fun fetchFavoriteWallpapers(apiKey: String, collectionName: String? = null): List<WallpaperDetail>
    fun toggleFavorite(id: String)
    fun loadFavorites(): Set<String>
    fun isFavorite(id: String): Boolean
    fun getCollections(): List<FavoriteCollection>
    fun createCollection(name: String): Boolean
    fun addToCollection(collectionName: String, wallpaperId: String)
}
