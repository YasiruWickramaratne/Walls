package com.example.walls

import FavoritesManager
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WallpaperViewModelFactory(
    private val application: Application,
    private val favoritesManager: FavoritesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WallpaperViewModel::class.java)) {
            val wallpaperRepository = WallpaperRepositoryImpl(application)
            val favoritesRepository = FavoritesRepositoryImpl(application, favoritesManager)
            @Suppress("UNCHECKED_CAST")
            return WallpaperViewModel(application, wallpaperRepository, favoritesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}