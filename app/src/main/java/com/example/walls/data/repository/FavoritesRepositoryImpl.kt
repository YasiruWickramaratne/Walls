package com.example.walls.data.repository

import com.example.walls.data.local.FavoritesManager
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

    override fun addFavorite(id: String): Boolean {
        return favoritesManager.addFavorite(id)
    }

    override fun loadFavorites(): Set<String> {
        return favoritesManager.getFavorites()
    }

    override fun getFavoriteIds(): Set<String> {
        return favoritesManager.getFavorites()
    }

    override fun isFavorite(id: String): Boolean {
        return favoritesManager.isFavorite(id)
    }

    override fun getCollections(): List<FavoriteCollection> {
        return favoritesManager.getCollections()
    }

    override fun createCollection(name: String): Boolean {
        return favoritesManager.createCollection(name)
    }

    override fun deleteCollection(name: String): Boolean {
        return favoritesManager.deleteCollection(name)
    }

    override fun getCollectionWallpaperIds(collectionNames: Set<String>): Set<String> {
        return favoritesManager.getCollectionWallpaperIds(collectionNames)
    }

    override fun addToCollection(collectionName: String, wallpaperId: String) {
        favoritesManager.addToCollection(collectionName, wallpaperId)
    }

    override fun toggleCollectionMembership(collectionName: String, wallpaperId: String): Boolean {
        return favoritesManager.toggleCollectionMembership(collectionName, wallpaperId)
    }

    override suspend fun fetchFavoriteWallpapers(apiKey: String, collectionName: String?): List<WallpaperDetail> {
        val favoriteIds = if (collectionName.isNullOrBlank()) {
            favoritesManager.getFavorites()
        } else {
            favoritesManager.getCollections()
                .firstOrNull { it.name == collectionName }
                ?.wallpaperIds
                .orEmpty()
        }
        return favoriteIds.mapNotNull { id ->
            try {
                apiService.getWallpaperDetails(id, apiKey).data
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun fetchWallpapersByIds(apiKey: String, wallpaperIds: Set<String>): List<WallpaperDetail> {
        return wallpaperIds.mapNotNull { id ->
            try {
                apiService.getWallpaperDetails(id, apiKey).data
            } catch (e: Exception) {
                null
            }
        }
    }
}
