package com.example.walls.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.example.walls.data.model.CollectionStylePreset
import com.example.walls.data.repository.FavoriteCollection
import com.google.gson.Gson
import javax.inject.Inject

class FavoritesManager @Inject constructor(
    context: Context
) {
    companion object {
        private const val MAX_COLLECTIONS = 10
    }

    private val favoritesPreferences: SharedPreferences =
        context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
    private val _favorites = MutableLiveData<Set<String>>()
    private val gson = Gson()

    private data class StoredFavoriteCollection(
        val name: String? = null,
        val wallpaperIds: Set<String>? = null,
        val stylePreset: String? = null
    )

    fun loadFavorites() {
        _favorites.postValue(getFavorites())
    }

    fun getFavorites(): Set<String> {
        return favoritesPreferences.getStringSet("favorite_ids", setOf()) ?: setOf()
    }

    fun getCollectionWallpaperIds(collectionNames: Set<String>): Set<String> {
        if (collectionNames.isEmpty()) return emptySet()
        val selectedNames = collectionNames.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (selectedNames.isEmpty()) return emptySet()

        return getCollections()
            .filter { collection -> selectedNames.any { it.equals(collection.name, ignoreCase = true) } }
            .flatMap { it.wallpaperIds }
            .toSet()
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

    fun addFavorite(id: String): Boolean {
        val currentFavorites = getFavorites().toMutableSet()
        val added = currentFavorites.add(id)
        if (added) {
            saveFavorites(currentFavorites)
            _favorites.postValue(currentFavorites)
        }
        return added
    }

    private fun saveFavorites(favorites: Set<String>) {
        favoritesPreferences.edit().putStringSet("favorite_ids", favorites).apply()
    }

    fun isFavorite(id: String): Boolean {
        return getFavorites().contains(id)
    }

    fun getCollections(): List<FavoriteCollection> {
        val json = favoritesPreferences.getString("favorite_collections", null) ?: return emptyList()
        return runCatching {
            gson.fromJson(json, Array<StoredFavoriteCollection>::class.java)
                ?.mapNotNull { stored ->
                    val name = stored.name?.trim().orEmpty()
                    if (name.isBlank()) {
                        null
                    } else {
                        FavoriteCollection(
                            name = name,
                            wallpaperIds = stored.wallpaperIds.orEmpty(),
                            stylePreset = stored.stylePreset
                                ?.let { presetName ->
                                    CollectionStylePreset.entries.firstOrNull { it.name == presetName }
                                }
                                ?: CollectionStylePreset.DEFAULT
                        )
                    }
                }
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    fun createCollection(name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isBlank()) return false
        val collections = getCollections().toMutableList()
        if (collections.any { it.name.equals(normalized, ignoreCase = true) }) return false
        if (collections.size >= MAX_COLLECTIONS) return false
        collections.add(FavoriteCollection(normalized, emptySet()))
        saveCollections(collections)
        return true
    }

    fun deleteCollection(name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isBlank()) return false

        val collections = getCollections().toMutableList()
        val removed = collections.removeAll { it.name.equals(normalized, ignoreCase = true) }
        if (!removed) return false

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
            collections.add(FavoriteCollection(normalized, setOf(wallpaperId)))
        }
        saveCollections(collections)
    }

    fun toggleCollectionMembership(collectionName: String, wallpaperId: String): Boolean {
        val normalized = collectionName.trim()
        if (normalized.isBlank()) return false

        val collections = getCollections().toMutableList()
        val index = collections.indexOfFirst { it.name.equals(normalized, ignoreCase = true) }
        if (index == -1) return false

        val collection = collections[index]
        val isCurrentlyIncluded = wallpaperId in collection.wallpaperIds
        collections[index] = collection.copy(
            wallpaperIds = if (isCurrentlyIncluded) {
                collection.wallpaperIds - wallpaperId
            } else {
                collection.wallpaperIds + wallpaperId
            }
        )
        saveCollections(collections)
        return !isCurrentlyIncluded
    }

    fun updateCollectionStyle(collectionName: String, stylePreset: CollectionStylePreset): Boolean {
        val normalized = collectionName.trim()
        if (normalized.isBlank()) return false

        val collections = getCollections().toMutableList()
        val index = collections.indexOfFirst { it.name.equals(normalized, ignoreCase = true) }
        if (index == -1) return false

        collections[index] = collections[index].copy(stylePreset = stylePreset)
        saveCollections(collections)
        return true
    }

    private fun saveCollections(collections: List<FavoriteCollection>) {
        favoritesPreferences.edit()
            .putString("favorite_collections", gson.toJson(collections))
            .apply()
    }
}
