package com.example.walls.data.manager

import com.example.walls.api.WallpaperDetail
import com.example.walls.data.model.CollectionStylePreset
import com.example.walls.data.repository.FavoriteCollection
import com.example.walls.data.repository.FavoritesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesCollectionManager @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) {
    suspend fun fetchFavoriteWallpapers(apiKey: String, collectionName: String? = null): List<WallpaperDetail> {
        return favoritesRepository.fetchFavoriteWallpapers(apiKey, collectionName)
    }

    suspend fun fetchWallpapersByIds(apiKey: String, wallpaperIds: Set<String>): List<WallpaperDetail> {
        return favoritesRepository.fetchWallpapersByIds(apiKey, wallpaperIds)
    }

    fun loadFavorites(): Set<String> = favoritesRepository.loadFavorites()

    fun isFavorite(id: String): Boolean = favoritesRepository.isFavorite(id)

    fun toggleFavorite(id: String) {
        favoritesRepository.toggleFavorite(id)
    }

    fun addFavorite(id: String): Boolean {
        return favoritesRepository.addFavorite(id)
    }

    fun getFavoriteIds(): Set<String> = favoritesRepository.getFavoriteIds()

    fun getCollections(): List<FavoriteCollection> = favoritesRepository.getCollections()

    fun createCollection(name: String): Boolean = favoritesRepository.createCollection(name)

    fun deleteCollection(name: String): Boolean = favoritesRepository.deleteCollection(name)

    fun addToCollection(collectionName: String, wallpaperId: String) {
        favoritesRepository.addToCollection(collectionName, wallpaperId)
    }

    fun toggleCollectionMembership(collectionName: String, wallpaperId: String): Boolean {
        return favoritesRepository.toggleCollectionMembership(collectionName, wallpaperId)
    }

    fun getCollectionWallpaperIds(collectionNames: Set<String>): Set<String> {
        return favoritesRepository.getCollectionWallpaperIds(collectionNames)
    }

    fun updateCollectionStyle(collectionName: String, stylePreset: CollectionStylePreset): Boolean {
        return favoritesRepository.updateCollectionStyle(collectionName, stylePreset)
    }
}
