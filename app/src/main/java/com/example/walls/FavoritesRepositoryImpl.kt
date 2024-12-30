package com.example.walls

import android.content.Context
import android.content.SharedPreferences

class FavoritesRepositoryImpl(
    private val context: Context,
    private val favoritesManager: FavoritesManager
) : FavoritesRepository {

    private val favoritesPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
    }

    override suspend fun fetchFavoriteWallpapers(apiKey: String): List<FavoritesManager.WallpaperDetail> {
        return favoritesManager.fetchFavoriteWallpapers(apiKey)
    }

    override fun toggleFavorite(id: String) {
        favoritesManager.toggleFavorite(id)
    }

    override fun loadFavorites(): Set<String> {
        return favoritesPreferences.getStringSet("favorite_ids", setOf()) ?: setOf()
    }

    override fun isFavorite(id: String): Boolean {
        return favoritesManager.isFavorite(id)
    }
}