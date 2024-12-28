import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class FavoritesManager(context: Context) {
    private val favoritesPreferences: SharedPreferences = context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
    private val _favorites = MutableLiveData<Set<String>>(setOf())

    private val apiService: WallhavenApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://wallhaven.cc/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WallhavenApiService::class.java)
    }

    init {
        loadFavorites()
    }

    fun toggleFavorite(id: String) {
        val currentFavorites = _favorites.value?.toMutableSet() ?: mutableSetOf()
        if (currentFavorites.contains(id)) {
            currentFavorites.remove(id)
        } else {
            currentFavorites.add(id)
        }
        _favorites.value = currentFavorites
        saveFavorites(currentFavorites)
    }

    fun isFavorite(id: String): Boolean {
        return _favorites.value?.contains(id) ?: false
    }

    private fun saveFavorites(favorites: Set<String>) {
        favoritesPreferences.edit().putStringSet("favorite_ids", favorites).apply()
    }

    private fun loadFavorites() {
        _favorites.value = favoritesPreferences.getStringSet("favorite_ids", setOf()) ?: setOf()
    }

    suspend fun fetchFavoriteWallpapers(apiKey: String): List<WallpaperDetail> {
        return withContext(Dispatchers.IO) {
            val favoriteIds = _favorites.value ?: setOf()
            favoriteIds.mapNotNull { id ->
                try {
                    apiService.getWallpaperDetails(id, apiKey).data
                } catch (e: Exception) {
                    null
                }
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