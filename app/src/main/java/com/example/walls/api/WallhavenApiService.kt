package com.example.walls.api

import com.example.walls.WallpaperResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WallhavenApiService {
    @GET("search")
    suspend fun searchWallpapers(
        @Query("apikey") apiKey: String?,
        @Query("sorting") sorting: String,
        @Query("categories") categories: String,
        @Query("purity") purity: String
    ): WallpaperResponse

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