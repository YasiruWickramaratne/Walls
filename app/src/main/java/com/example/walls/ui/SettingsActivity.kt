package com.example.walls.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.walls.ThemeMode
import com.example.walls.WallpaperViewModel
import com.example.walls.data.manager.AutoWallpaperSettingsManager
import com.example.walls.ui.screens.SettingsScreen
import com.example.walls.ui.theme.WallsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: WallpaperViewModel by viewModels()
    @Inject lateinit var autoWallpaperSettingsManager: AutoWallpaperSettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            WallsTheme(darkTheme = isDark) {
                SettingsScreen(
                    viewModel = viewModel,
                    autoWallpaperSettingsManager = autoWallpaperSettingsManager,
                    onBack = { finish() }
                )
            }
        }
    }
}
