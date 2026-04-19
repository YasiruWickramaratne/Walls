package com.example.walls.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class FavoritesManager @Inject constructor(
    context: Context
) {
    private val favoritesPreferences: SharedPreferences =
        context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
    private val _favorites = MutableLiveData<Set<String>>()
    private val gson = Gson()

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

    fun getCollections(): List<com.example.walls.data.repository.FavoriteCollection> {
        val json = favoritesPreferences.getString("favorite_collections", null) ?: return emptyList()
        val type = object : TypeToken<List<com.example.walls.data.repository.FavoriteCollection>>() {}.type
        return runCatching {
            gson.fromJson<List<com.example.walls.data.repository.FavoriteCollection>>(json, type)
        }.getOrDefault(emptyList())
    }

    fun createCollection(name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isBlank()) return false
        val collections = getCollections().toMutableList()
        if (collections.any { it.name.equals(normalized, ignoreCase = true) }) return false
        collections.add(com.example.walls.data.repository.FavoriteCollection(normalized, emptySet()))
        saveCollections(collections)
        return true
    }

    fun addToCollection(collectionName: String, wallpaperId: String) {
        val normalized = collectionName.trim()
        if (normalized.isBlank()) return
        val collections = getCollections().toMutableList()
        val index = collections.indexOfFirst { it.name.equals(normalized, ignoreCase = true) }
        if (index >= 0) {
            val collection = collections[index]
            collections[index] = collection.copy(wallpaperIds = collection.wallpaperIds + wallpaperId)
        } else {
            collections.add(com.example.walls.data.repository.FavoriteCollection(normalized, setOf(wallpaperId)))
        }
        saveCollections(collections)
    }

    private fun saveCollections(collections: List<com.example.walls.data.repository.FavoriteCollection>) {
        favoritesPreferences.edit()
            .putString("favorite_collections", gson.toJson(collections))
            .apply()
    }
}
