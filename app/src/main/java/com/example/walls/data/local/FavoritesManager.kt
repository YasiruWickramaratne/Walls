package com.example.walls.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject

class FavoritesManager @Inject constructor(
    context: Context
) {
    private val favoritesPreferences: SharedPreferences =
        context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
    private val _favorites = MutableLiveData<Set<String>>()

    fun loadFavorites() {
        _favorites.postValue(getFavorites())
    }

    fun getFavorites(): Set<String> {
        return favoritesPreferences.getStringSet("favorite_ids", setOf()) ?: setOf()
    }

    fun toggleFavorite(id: String) {
        val currentFavorites = getFavorites().toMutableSet()
        if (currentFavorites.contains(id)) {
            currentFavorites.remove(id)
        } else {
            currentFavorites.add(id)
        }
        saveFavorites(currentFavorites)
        _favorites.postValue(currentFavorites)
    }

    private fun saveFavorites(favorites: Set<String>) {
        favoritesPreferences.edit().putStringSet("favorite_ids", favorites).apply()
    }

    fun isFavorite(id: String): Boolean {
        return getFavorites().contains(id)
    }
}