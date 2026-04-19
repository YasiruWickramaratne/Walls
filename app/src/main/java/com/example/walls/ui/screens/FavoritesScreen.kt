package com.example.walls.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.walls.Thumbs
import com.example.walls.Wallpaper
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.CropActivity
import com.example.walls.ui.components.WallpaperCard
import com.example.walls.ui.favorites.FavoritesCollectionChips
import com.example.walls.ui.favorites.FavoritesDialogs
import com.example.walls.ui.favorites.FavoritesSelectionActions
import com.example.walls.ui.favorites.FavoritesTopBar
import com.example.walls.ui.favorites.FavoritesUiMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(viewModel: WallpaperViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val favoriteWallpapers by viewModel.favoriteWallpapers.collectAsStateWithLifecycle()
    val collections by viewModel.favoriteCollections.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedFavoritesCollection.collectAsStateWithLifecycle()
    var uiMode by remember(selectedCollection) {
        mutableStateOf<FavoritesUiMode>(
            selectedCollection?.let(FavoritesUiMode::BrowsingCollection) ?: FavoritesUiMode.BrowsingDefault
        )
    }
    var showBulkRemoveConfirmation by remember { mutableStateOf(false) }
    var showDeleteCollectionConfirmation by remember { mutableStateOf(false) }
    var showDeleteEmptyCollectionPrompt by remember { mutableStateOf(false) }

    val wallpapers = favoriteWallpapers.map { detail ->
        Wallpaper(
            id = detail.id,
            url = detail.url,
            path = detail.path,
            thumbs = Thumbs(
                large = detail.thumbs.large,
                original = detail.thumbs.original,
                small = detail.thumbs.small
            )
        )
    }

    val selectedIds = (uiMode as? FavoritesUiMode.Selecting)?.ids.orEmpty()
    val isSelectionMode = uiMode is FavoritesUiMode.Selecting
    val activeCollectionName = when (val mode = uiMode) {
        FavoritesUiMode.BrowsingDefault -> null
        is FavoritesUiMode.BrowsingCollection -> mode.name
        is FavoritesUiMode.Selecting -> mode.collectionName
    }

    Scaffold(
        topBar = {
            FavoritesTopBar(
                mode = uiMode,
                onBack = onBack,
                onExitSelection = {
                    uiMode = activeCollectionName?.let(FavoritesUiMode::BrowsingCollection)
                        ?: FavoritesUiMode.BrowsingDefault
                },
                onDeleteCollection = { showDeleteCollectionConfirmation = true }
            )
        },
        floatingActionButton = {
            FavoritesSelectionActions(
                visible = isSelectionMode,
                isDefaultSelection = activeCollectionName == null,
                onClick = { showBulkRemoveConfirmation = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FavoritesCollectionChips(
                collections = collections,
                selectedCollection = selectedCollection,
                onSelectDefault = {
                    uiMode = FavoritesUiMode.BrowsingDefault
                    viewModel.selectFavoritesCollection(null)
                },
                onSelectCollection = { collectionName ->
                    uiMode = FavoritesUiMode.BrowsingCollection(collectionName)
                    viewModel.selectFavoritesCollection(collectionName)
                }
            )

            if (wallpapers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (selectedCollection == null) "No favorites yet"
                        else "No wallpapers in ${selectedCollection}"
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(wallpapers, key = { it.id }) { wallpaper ->
                        WallpaperCard(
                            wallpaper = wallpaper,
                            onClick = {
                                if (isSelectionMode) {
                                    val updatedSelection = if (selectedIds.contains(wallpaper.id)) {
                                        selectedIds - wallpaper.id
                                    } else {
                                        selectedIds + wallpaper.id
                                    }
                                    uiMode = if (updatedSelection.isEmpty()) {
                                        activeCollectionName?.let(FavoritesUiMode::BrowsingCollection)
                                            ?: FavoritesUiMode.BrowsingDefault
                                    } else {
                                        FavoritesUiMode.Selecting(
                                            ids = updatedSelection,
                                            collectionName = activeCollectionName
                                        )
                                    }
                                } else {
                                    Intent(context, CropActivity::class.java).apply {
                                        putExtra("WALLPAPER_ID", wallpaper.id)
                                        putExtra("IMAGE_URL", wallpaper.path)
                                        putExtra("COLLECTION_NAME", selectedCollection)
                                    }.also { context.startActivity(it) }
                                }
                            },
                            onLongClick = {
                                uiMode = FavoritesUiMode.Selecting(
                                    ids = selectedIds + wallpaper.id,
                                    collectionName = activeCollectionName
                                )
                            },
                            selected = selectedIds.contains(wallpaper.id)
                        )
                    }
                }
            }
        }
    }

    FavoritesDialogs(
        showBulkRemoveConfirmation = showBulkRemoveConfirmation,
        showDeleteCollectionConfirmation = showDeleteCollectionConfirmation,
        showDeleteEmptyCollectionPrompt = showDeleteEmptyCollectionPrompt,
        selectedCollection = selectedCollection,
        isDefaultSelection = activeCollectionName == null,
        onDismissBulkRemove = { showBulkRemoveConfirmation = false },
        onConfirmBulkRemove = {
            val selectedCount = selectedIds.size
            val currentCollectionName = activeCollectionName
            selectedIds.forEach { wallpaperId ->
                if (currentCollectionName == null) {
                    viewModel.toggleFavorite(wallpaperId)
                } else {
                    viewModel.toggleWallpaperInCollection(currentCollectionName, wallpaperId)
                }
            }
            viewModel.fetchFavoriteWallpapers()
            showBulkRemoveConfirmation = false
            uiMode = currentCollectionName?.let(FavoritesUiMode::BrowsingCollection)
                ?: FavoritesUiMode.BrowsingDefault
            if (currentCollectionName != null && selectedCount == wallpapers.size) {
                showDeleteEmptyCollectionPrompt = true
            }
        },
        onDismissDeleteCollection = { showDeleteCollectionConfirmation = false },
        onConfirmDeleteCollection = {
            selectedCollection?.let(viewModel::deleteFavoriteCollection)
            viewModel.selectFavoritesCollection(null)
            showDeleteCollectionConfirmation = false
            uiMode = FavoritesUiMode.BrowsingDefault
        },
        onDismissDeleteEmptyCollection = { showDeleteEmptyCollectionPrompt = false },
        onConfirmDeleteEmptyCollection = {
            selectedCollection?.let(viewModel::deleteFavoriteCollection)
            viewModel.selectFavoritesCollection(null)
            showDeleteEmptyCollectionPrompt = false
            uiMode = FavoritesUiMode.BrowsingDefault
        }
    )
}
