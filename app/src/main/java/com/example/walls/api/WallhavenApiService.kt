package com.example.walls.api

import com.example.walls.WallpaperResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WallhavenApiService {
    @GET("search")
    suspend fun searchWallpapers(
        @Query("apikey") apiKey: String?,
        @Query("q") query: String? = null,
        @Query("sorting") sorting: String,
        @Query("categories") categories: String,
        @Query("purity") purity: String,
        @Query("resolutions") resolutions: String? = null,
        @Query("ratios") ratios: String? = null,
        @Query("colors") colors: String? = null,
        @Query("page") page: Int = 1
    ): WallpaperResponse

    @GET("w/{id}")
    suspend fun getWallpaperDetails(
        @Path("id") id: String,
        @Query("apikey") apiKey: String? = null
    ): WallpaperDetailResponse
}

data class WallpaperDetailResponse(
    val data: WallpaperDetail
)

data class WallpaperDetail(
    val id: String,
    val url: String,
    val path: String,
    val views: Int = 0,
    val favorites: Int = 0,
    val category: String = "",
    val purity: String = "",
    val dimension_x: Int = 0,
    val dimension_y: Int = 0,
    val resolution: String,
    val file_size: Int,
    val file_type: String = "",
    val created_at: String = "",
    val colors: List<String>,
    val tags: List<WallpaperTag> = emptyList(),
    val thumbs: Thumbs
)

data class WallpaperTag(
    val id: Int,
    val name: String,
    val category: String? = null
)

data class Thumbs(
    val large: String,
    val original: String,
    val small: String
) 
