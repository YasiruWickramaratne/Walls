package com.example.walls.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.walls.Wallpaper
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.FullScreenImageActivity
import com.example.walls.ui.dialogs.FilterDialog
import com.example.walls.ui.components.WallpaperCard
import com.example.walls.ui.fullscreen.components.AddToCollectionDialog
import com.google.gson.Gson
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val favoriteCollections by viewModel.favoriteCollections.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIds by rememberSaveable(activeQuery) { mutableStateOf(setOf<String>()) }
    val typedQuery = query.trim()
    val displayQuery = activeQuery.ifBlank { typedQuery }
    val isSelectionMode = selectedIds.isNotEmpty()
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    fun submitSearch() {
        viewModel.fetchSearchWallpapers(query)
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        selectedIds = emptySet()
    }

    fun clearSelection() {
        selectedIds = emptySet()
    }

    fun addSelectedToFavorites() {
        selectedIds.forEach(viewModel::addFavorite)
        Toast.makeText(
            context,
            if (selectedIds.size == 1) "Added to favorites" else "Added ${selectedIds.size} wallpapers to favorites",
            Toast.LENGTH_SHORT
        ).show()
        clearSelection()
    }

    LaunchedEffect(initialQuery, activeQuery) {
        val preferredQuery = initialQuery.takeIf { it.isNotBlank() } ?: activeQuery
        query = preferredQuery
        if (preferredQuery.isNotBlank() && searchWallpapers.isEmpty()) {
            viewModel.fetchSearchWallpapers(preferredQuery)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectionMode) "${selectedIds.size} selected" else "Search") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            clearSelection()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(onClick = { clearSelection() }) {
                            Text("Cancel")
                        }
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
                    ),
                    colors = textFieldColors
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
                            SearchWallpaperGrid(
                                wallpapers = searchWallpapers,
                                selectedIds = selectedIds,
                                onWallpaperClick = { wallpaper, index ->
                                    if (isSelectionMode) {
                                        selectedIds = if (selectedIds.contains(wallpaper.id)) {
                                            selectedIds - wallpaper.id
                                        } else {
                                            selectedIds + wallpaper.id
                                        }
                                    } else {
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
                                    }
                                },
                                onWallpaperLongClick = { wallpaper ->
                                    selectedIds = selectedIds + wallpaper.id
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
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSelectionMode) {
                    FloatingActionButton(
                        onClick = { showCollectionDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation()
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Add to collection")
                    }
                    FloatingActionButton(
                        onClick = { addSelectedToFavorites() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation()
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Add to favorites")
                    }
                } else {
                    FloatingActionButton(
                        onClick = { showFilterDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation()
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }

                    FloatingActionButton(
                        onClick = { submitSearch() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
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

    if (showCollectionDialog && selectedIds.isNotEmpty()) {
        AddToCollectionDialog(
            collections = favoriteCollections,
            wallpaperIds = selectedIds,
            onDismiss = { showCollectionDialog = false },
            onCreateCollection = { name ->
                val created = viewModel.createFavoriteCollection(name)
                if (created) {
                    selectedIds.forEach { wallpaperId ->
                        viewModel.addWallpaperToCollection(name, wallpaperId)
                    }
                    Toast.makeText(
                        context,
                        if (selectedIds.size == 1) "Added to $name" else "Added ${selectedIds.size} wallpapers to $name",
                        Toast.LENGTH_SHORT
                    ).show()
                    showCollectionDialog = false
                    clearSelection()
                } else {
                    val message = when {
                        favoriteCollections.any { it.name.equals(name, ignoreCase = true) } ->
                            "Collection name already exists"
                        favoriteCollections.size >= 10 ->
                            "You can create up to 10 collections"
                        else ->
                            "Unable to create collection"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onToggleCollection = { name ->
                selectedIds.forEach { wallpaperId ->
                    viewModel.toggleWallpaperInCollection(name, wallpaperId)
                }
                Toast.makeText(
                    context,
                    "Updated $name",
                    Toast.LENGTH_SHORT
                ).show()
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
            columns = GridCells.Fixed(3),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchWallpaperGrid(
    wallpapers: List<Wallpaper>,
    selectedIds: Set<String>,
    onWallpaperClick: (Wallpaper, Int) -> Unit,
    onWallpaperLongClick: (Wallpaper) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(wallpapers, key = { _, wallpaper -> wallpaper.id }) { index, wallpaper ->
            if (index >= wallpapers.size - 5) {
                LaunchedEffect(index) { onLoadMore() }
            }
            WallpaperCard(
                wallpaper = wallpaper,
                onClick = { onWallpaperClick(wallpaper, index) },
                onLongClick = { onWallpaperLongClick(wallpaper) },
                selected = selectedIds.contains(wallpaper.id)
            )
        }
    }
}
