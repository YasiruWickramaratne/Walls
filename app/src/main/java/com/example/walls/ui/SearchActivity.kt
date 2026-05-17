package com.example.walls.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.walls.ThemeMode
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.screens.SearchScreen
import com.example.walls.ui.theme.WallsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_INITIAL_QUERY = "EXTRA_INITIAL_QUERY"

        fun createIntent(context: android.content.Context, query: String): Intent {
            return Intent(context, SearchActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_QUERY, query)
            }
        }
    }

    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialQuery = intent.getStringExtra(EXTRA_INITIAL_QUERY).orEmpty().trim()
        viewModel.resetSearchFilters(clearResults = false)

        if (initialQuery.isNotBlank()) {
            viewModel.fetchSearchWallpapers(initialQuery)
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
                SearchScreen(
                    viewModel = viewModel,
                    initialQuery = initialQuery,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            viewModel.resetSearchFilters(clearResults = true)
        }
        super.onDestroy()
    }
}
