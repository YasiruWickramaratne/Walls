package com.example.walls.di

import android.content.Context
import com.example.walls.FavoritesManager
import com.example.walls.api.WallhavenApiService
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
        fun provideFavoritesManager(@ApplicationContext context: Context, apiService: WallhavenApiService): FavoritesManager {
            return FavoritesManager(context, apiService)
        }
    }
} 