package com.example.walls

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject

class FavoritesManager @Inject constructor(context: Context) {
    private val favoritesPreferences: SharedPreferences =
        context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
    private val _favorites = MutableLiveData<Set<String>>(setOf())

    private val apiService: WallhavenApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://wallhaven.cc/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WallhavenApiService::class.java)
    }

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

    interface WallhavenApiService {
        @GET("w/{id}")
        suspend fun getWallpaperDetails(
            @Path("id") id: String,
            @Query("apikey") apiKey: String
        ): WallpaperDetailResponse
    }

    data class WallpaperDetailResponse(
        val data: WallpaperDetail
    )

    data class WallpaperDetail(
        val id: String,
        val url: String,
        val path: String,
        val resolution: String,
        val file_size: Int,
        val colors: List<String>,
        val thumbs: Thumbs
    )

    data class Thumbs(
        val large: String,
        val original: String,
        val small: String
    )
}