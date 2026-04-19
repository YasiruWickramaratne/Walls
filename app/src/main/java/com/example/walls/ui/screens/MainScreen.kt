package com.example.walls.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.walls.Wallpaper
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.FavoritesActivity
import com.example.walls.ui.FullScreenImageActivity
import com.example.walls.ui.SettingsActivity
import com.example.walls.ui.components.WallpaperCard
import com.google.gson.Gson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: WallpaperViewModel) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showFilterDialog by remember { mutableStateOf(false) }

    val recentWallpapers by viewModel.recentWallpapers.collectAsStateWithLifecycle()
    val topWallpapers by viewModel.topWallpapers.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(Unit) {
        viewModel.fetchWallpapers("date_added")
        viewModel.fetchWallpapers("toplist")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Favorites") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, FavoritesActivity::class.java))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Walls") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("Recent") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Top") }
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val wallpapers = if (page == 0) recentWallpapers else topWallpapers
                    val sorting = if (page == 0) "date_added" else "toplist"
                    WallpaperGrid(
                        wallpapers = wallpapers,
                        onWallpaperClick = { wallpaper, index ->
                            val wallpapersJson = Gson().toJson(wallpapers)
                            Intent(context, FullScreenImageActivity::class.java).apply {
                                putExtra("WALLPAPER_ID", wallpaper.id)
                                putExtra("IMAGE_URL", wallpaper.path)
                                putExtra("WALLPAPER_LIST", wallpapersJson)
                                putExtra("WALLPAPER_INDEX", index)
                                putExtra("WALLPAPER_SORTING", sorting)
                                putExtra("WALLPAPER_NEXT_PAGE", viewModel.getNextPageForSorting(sorting))
                                putExtra("WALLPAPER_HAS_MORE", viewModel.hasMorePagesForSorting(sorting))
                            }.also { context.startActivity(it) }
                        },
                        onLoadMore = { viewModel.fetchWallpapers(sorting, isLoadingMore = true) }
                    )
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            viewModel = viewModel,
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
fun WallpaperGrid(
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (Wallpaper, Int) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (wallpapers.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading wallpapers...")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(wallpapers) { index, wallpaper ->
            if (index >= wallpapers.size - 5) {
                LaunchedEffect(index) { onLoadMore() }
            }
            WallpaperCard(
                wallpaper = wallpaper,
                onClick = { onWallpaperClick(wallpaper, index) }
            )
        }
    }
}

@Composable
fun FilterDialog(viewModel: WallpaperViewModel, onDismiss: () -> Unit) {
    var general by remember { mutableStateOf(viewModel.isGeneralSelected()) }
    var anime by remember { mutableStateOf(viewModel.isAnimeSelected()) }
    var people by remember { mutableStateOf(viewModel.isPeopleSelected()) }
    var sfw by remember { mutableStateOf(viewModel.isSfwSelected()) }
    var sketchy by remember { mutableStateOf(viewModel.isSketchySelected()) }
    var nsfw by remember { mutableStateOf(viewModel.isNsfwSelected()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Wallpapers") },
        text = {
            Column {
                Text("Categories")
                ListItem(
                    headlineContent = { Text("General") },
                    leadingContent = { Checkbox(checked = general, onCheckedChange = { general = it }) }
                )
                ListItem(
                    headlineContent = { Text("Anime") },
                    leadingContent = { Checkbox(checked = anime, onCheckedChange = { anime = it }) }
                )
                ListItem(
                    headlineContent = { Text("People") },
                    leadingContent = { Checkbox(checked = people, onCheckedChange = { people = it }) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Purity")
                ListItem(
                    headlineContent = { Text("SFW") },
                    leadingContent = { Checkbox(checked = sfw, onCheckedChange = { sfw = it }) }
                )
                ListItem(
                    headlineContent = { Text("Sketchy") },
                    leadingContent = { Checkbox(checked = sketchy, onCheckedChange = { sketchy = it }) }
                )
                ListItem(
                    headlineContent = { Text("NSFW") },
                    leadingContent = { Checkbox(checked = nsfw, onCheckedChange = { nsfw = it }) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val categories = "${if (general) "1" else "0"}${if (anime) "1" else "0"}${if (people) "1" else "0"}"
                val purity = "${if (sfw) "1" else "0"}${if (sketchy) "1" else "0"}${if (nsfw) "1" else "0"}"
                viewModel.updateFilters(categories, purity)
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
