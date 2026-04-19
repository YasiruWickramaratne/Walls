package com.example.walls.ui.screens

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: WallpaperViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val favoriteWallpapers by viewModel.favoriteWallpapers.collectAsStateWithLifecycle()
    val collections by viewModel.favoriteCollections.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedFavoritesCollection.collectAsStateWithLifecycle()
    val chipScroll = rememberScrollState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedCollection ?: "Favorites") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScroll)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCollection == null,
                    onClick = { viewModel.selectFavoritesCollection(null) },
                    label = { Text("Default") }
                )
                collections.forEach { collection ->
                    FilterChip(
                        selected = selectedCollection == collection.name,
                        onClick = { viewModel.selectFavoritesCollection(collection.name) },
                        label = { Text(collection.name) }
                    )
                }
            }

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
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(wallpapers, key = { it.id }) { wallpaper ->
                        WallpaperCard(
                            wallpaper = wallpaper,
                            onClick = {
                                Intent(context, CropActivity::class.java).apply {
                                    putExtra("WALLPAPER_ID", wallpaper.id)
                                    putExtra("IMAGE_URL", wallpaper.path)
                                }.also { context.startActivity(it) }
                            }
                        )
                    }
                }
            }
        }
    }
}
