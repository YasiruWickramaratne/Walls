package com.example.walls.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.FullScreenImageActivity
import com.google.gson.Gson
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WallpaperViewModel,
    initialQuery: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchWallpapers by viewModel.searchWallpapers.collectAsStateWithLifecycle()
    val activeQuery by viewModel.currentSearchQuery.collectAsStateWithLifecycle()
    val isSearchLoading by viewModel.searchLoading.collectAsStateWithLifecycle()
    val hasCompletedSearch by viewModel.hasCompletedSearch.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val typedQuery = query.trim()
    val displayQuery = activeQuery.ifBlank { typedQuery }

    fun submitSearch() {
        viewModel.fetchSearchWallpapers(query)
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    LaunchedEffect(initialQuery, activeQuery) {
        val preferredQuery = initialQuery.takeIf { it.isNotBlank() } ?: activeQuery
        query = preferredQuery
        if (preferredQuery.isNotBlank() && searchWallpapers.isEmpty()) {
            viewModel.fetchSearchWallpapers(preferredQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text("Search wallpapers") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { submitSearch() }
                    )
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        activeQuery.isBlank() && typedQuery.isBlank() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Type a search and press Enter",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        isSearchLoading && searchWallpapers.isEmpty() -> {
                            SearchLoadingGrid(query = displayQuery)
                        }

                        hasCompletedSearch && activeQuery.isNotBlank() && searchWallpapers.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No results for \"$displayQuery\"",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        activeQuery.isBlank() && typedQuery.isNotBlank() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Press search to find wallpapers for \"$typedQuery\"",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        else -> {
                            WallpaperGrid(
                                wallpapers = searchWallpapers,
                                onWallpaperClick = { wallpaper, index ->
                                    val wallpapersJson = Gson().toJson(searchWallpapers)
                                    Intent(context, FullScreenImageActivity::class.java).apply {
                                        putExtra("WALLPAPER_ID", wallpaper.id)
                                        putExtra("IMAGE_URL", wallpaper.path)
                                        putExtra("WALLPAPER_LIST", wallpapersJson)
                                        putExtra("WALLPAPER_INDEX", index)
                                        putExtra("WALLPAPER_SORTING", "search")
                                        putExtra("WALLPAPER_SEARCH_QUERY", displayQuery)
                                        putExtra("WALLPAPER_NEXT_PAGE", viewModel.getNextPageForSorting("search"))
                                        putExtra("WALLPAPER_HAS_MORE", viewModel.hasMorePagesForSorting("search"))
                                    }.also { context.startActivity(it) }
                                },
                                onLoadMore = { viewModel.fetchSearchWallpapers(displayQuery, isLoadingMore = true) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }

                FloatingActionButton(
                    onClick = { submitSearch() }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            viewModel = viewModel,
            useSearchFilters = true,
            onDismiss = {
                showFilterDialog = false
                if (query.isNotBlank()) {
                    viewModel.fetchSearchWallpapers(query)
                }
            }
        )
    }
}

@Composable
private fun SearchLoadingGrid(query: String) {
    val colorScheme = MaterialTheme.colorScheme
    val placeholders = List(8) { it }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Searching for \"$query\"...",
            color = colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(placeholders) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .background(colorScheme.secondaryContainer.copy(alpha = 0.45f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(0.7f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colorScheme.onSurface.copy(alpha = 0.08f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(0.45f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colorScheme.onSurface.copy(alpha = 0.06f))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}
