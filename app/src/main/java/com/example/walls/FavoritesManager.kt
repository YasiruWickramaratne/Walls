package com.example.walls

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.example.walls.api.WallhavenApiService
import com.example.walls.api.WallpaperDetail
import javax.inject.Inject

class FavoritesManager @Inject constructor(
    context: Context,
    private val apiService: WallhavenApiService
) {
    private val favoritesPreferences: SharedPreferences =
        context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
    private val _favorites = MutableLiveData<Set<String>>(setOf())

    init {
        _favorites.value = getFavorites()
    }

    fun toggleFavorite(id: String) {
        val currentFavorites = getFavorites().toMutableSet()
        if (currentFavorites.contains(id)) {
            currentFavorites.remove(id)
        } else {
            currentFavorites.add(id)
        }
        saveFavorites(currentFavorites)
        _favorites.value = currentFavorites
    }

    fun getFavorites(): Set<String> {
        return favoritesPreferences.getStringSet("favorite_ids", setOf()) ?: setOf()
    }

    private fun saveFavorites(favorites: Set<String>) {
        favoritesPreferences.edit().putStringSet("favorite_ids", favorites).apply()
    }

    fun isFavorite(id: String): Boolean {
        return getFavorites().contains(id)
    }

    suspend fun fetchFavoriteWallpapers(apiKey: String): List<WallpaperDetail> {
        val favorites = getFavorites()
        return favorites.mapNotNull { id ->
            try {
                apiService.getWallpaperDetails(id, apiKey).data
            } catch (e: Exception) {
                null
            }
        }
    }
}