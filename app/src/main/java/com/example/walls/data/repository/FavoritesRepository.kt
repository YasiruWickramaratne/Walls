package com.example.walls.data.repository

import com.example.walls.data.model.CollectionStylePreset
import com.example.walls.api.WallpaperDetail

data class FavoriteCollection(
    val name: String,
    val wallpaperIds: Set<String>,
    val stylePreset: CollectionStylePreset = CollectionStylePreset.DEFAULT
)

interface FavoritesRepository {
    suspend fun fetchFavoriteWallpapers(apiKey: String, collectionName: String? = null): List<WallpaperDetail>
    suspend fun fetchWallpapersByIds(apiKey: String, wallpaperIds: Set<String>): List<WallpaperDetail>
    fun toggleFavorite(id: String)
    fun addFavorite(id: String): Boolean
    fun loadFavorites(): Set<String>
    fun isFavorite(id: String): Boolean
    fun getFavoriteIds(): Set<String>
    fun getCollections(): List<FavoriteCollection>
    fun getCollectionWallpaperIds(collectionNames: Set<String>): Set<String>
    fun createCollection(name: String): Boolean
    fun deleteCollection(name: String): Boolean
    fun addToCollection(collectionName: String, wallpaperId: String)
    fun toggleCollectionMembership(collectionName: String, wallpaperId: String): Boolean
    fun updateCollectionStyle(collectionName: String, stylePreset: CollectionStylePreset): Boolean
}
