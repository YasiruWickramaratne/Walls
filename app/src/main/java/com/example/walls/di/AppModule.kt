package com.example.walls.di


import android.content.Context
import com.example.walls.FavoritesManager
import com.example.walls.FavoritesRepository
import com.example.walls.FavoritesRepositoryImpl
import com.example.walls.WallpaperRepository
import com.example.walls.WallpaperRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFavoritesManager(@ApplicationContext context: Context): FavoritesManager {
        return FavoritesManager(context)
    }

    @Provides
    @Singleton
    fun provideWallpaperRepository(@ApplicationContext context: Context): WallpaperRepository {
        return WallpaperRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideFavoritesRepository(
        @ApplicationContext context: Context,
        favoritesManager: FavoritesManager
    ): FavoritesRepository {
        return FavoritesRepositoryImpl(context, favoritesManager)
    }
} 