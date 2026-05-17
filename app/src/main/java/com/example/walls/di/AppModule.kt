package com.example.walls.di

import android.content.Context
import androidx.room.Room
import com.example.walls.data.local.FavoritesManager
import com.example.walls.data.local.WallsDatabase
import com.example.walls.data.local.analysis.WallpaperAnalysisDao
import com.example.walls.data.repository.FavoritesRepository
import com.example.walls.data.repository.FavoritesRepositoryImpl
import com.example.walls.data.repository.WallpaperRepository
import com.example.walls.data.repository.WallpaperRepositoryImpl
import com.example.walls.api.WallhavenApiService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Singleton
    @Binds
    abstract fun bindWallpaperRepository(wallpaperRepositoryImpl: WallpaperRepositoryImpl): WallpaperRepository

    @Singleton
    @Binds
    abstract fun bindFavoritesRepository(favoritesRepositoryImpl: FavoritesRepositoryImpl): FavoritesRepository

    companion object {
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
        fun provideWallsDatabase(@ApplicationContext context: Context): WallsDatabase {
            return Room.databaseBuilder(
                context,
                WallsDatabase::class.java,
                "walls.db"
            ).build()
        }

        @Provides
        fun provideWallpaperAnalysisDao(database: WallsDatabase): WallpaperAnalysisDao {
            return database.wallpaperAnalysisDao()
        }
    }
} 
