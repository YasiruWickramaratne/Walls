package com.example.walls.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.Coil
import com.example.walls.ThemeMode
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.screens.MainScreen
import com.example.walls.ui.theme.WallsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: WallpaperViewModel by viewModels()
    private var hasLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isAmoled = themeMode == ThemeMode.AMOLED_DARK
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.AMOLED_DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            WallsTheme(darkTheme = isDark, amoledDark = isAmoled) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLaunched) viewModel.setReturningFromFullScreen()
        hasLaunched = true
        viewModel.refreshThemeMode()
        val oldQuality = viewModel.thumbnailQuality.value
        viewModel.refreshThumbnailQuality()
        if (viewModel.thumbnailQuality.value != oldQuality) {
            Coil.imageLoader(this).memoryCache?.clear()
        }
    }
}
