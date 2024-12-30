package com.example.walls.di

import android.content.Context
import com.example.walls.FavoritesManager
import com.example.walls.FavoritesRepository
import com.example.walls.FavoritesRepositoryImpl
import com.example.walls.WallpaperRepository
import com.example.walls.WallpaperRepositoryImpl
import com.example.walls.api.WallhavenApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideWallhavenApiService(): WallhavenApiService {
        return Retrofit.Builder()
            .baseUrl("https://wallhaven.cc/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WallhavenApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFavoritesManager(@ApplicationContext context: Context): FavoritesManager {
        return FavoritesManager(context)
    }

    @Provides
    @Singleton
    fun provideWallpaperRepositoryImpl(
        @ApplicationContext context: Context,
        apiService: WallhavenApiService
    ): WallpaperRepositoryImpl {
        return WallpaperRepositoryImpl(context, apiService)
    }

    @Provides
    @Singleton
    fun provideWallpaperRepository(impl: WallpaperRepositoryImpl): WallpaperRepository = impl

    @Provides
    @Singleton
    fun provideFavoritesRepositoryImpl(
        @ApplicationContext context: Context,
        favoritesManager: FavoritesManager
    ): FavoritesRepositoryImpl {
        return FavoritesRepositoryImpl(favoritesManager)
    }

    @Provides
    @Singleton
    fun provideFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository = impl
} 