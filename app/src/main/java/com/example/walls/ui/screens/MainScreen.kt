package com.example.walls.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.walls.Wallpaper
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.FavoritesActivity
import com.example.walls.ui.FullScreenImageActivity
import com.example.walls.ui.SearchActivity
import com.example.walls.ui.SettingsActivity
import com.example.walls.ui.components.WallpaperCard
import com.google.gson.Gson
import kotlinx.coroutines.launch

private val resolutionOptions = listOf(
    "1920x1080",
    "2560x1440",
    "3440x1440",
    "3840x2160",
    "5120x2880",
    "7680x4320",
    "1080x1920",
    "1440x2560"
)

private val wideResolutionOptions = resolutionOptions.filter { resolutionOrientation(it) == "Wide" }
private val portraitResolutionOptions = resolutionOptions.filter { resolutionOrientation(it) == "Portrait" }
private val squareResolutionOptions = resolutionOptions.filter { resolutionOrientation(it) == "Square" }

private val ratioOptions = listOf("Any", "16x9", "16x10", "21x9", "4x3", "5x4", "32x9", "9x16")

private val colorOptions = listOf(
    "660000", "cc3333", "ea4c88", "993399", "333399", "0066cc",
    "0099cc", "66cccc", "77cc33", "669900", "cccc33", "ffcc33",
    "ff9900", "ff6600", "996633", "000000", "999999", "cccccc", "ffffff", "424153"
)

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
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
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
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, SearchActivity::class.java))
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
                    title = { Text("wallP") },
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
            useSearchFilters = false,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(viewModel: WallpaperViewModel, onDismiss: () -> Unit) {
    FilterDialog(viewModel = viewModel, useSearchFilters = false, onDismiss = onDismiss)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    viewModel: WallpaperViewModel,
    useSearchFilters: Boolean,
    onDismiss: () -> Unit
) {
    var general by remember {
        mutableStateOf(if (useSearchFilters) viewModel.isSearchGeneralSelected() else viewModel.isGeneralSelected())
    }
    var anime by remember {
        mutableStateOf(if (useSearchFilters) viewModel.isSearchAnimeSelected() else viewModel.isAnimeSelected())
    }
    var people by remember {
        mutableStateOf(if (useSearchFilters) viewModel.isSearchPeopleSelected() else viewModel.isPeopleSelected())
    }
    var sfw by remember {
        mutableStateOf(if (useSearchFilters) viewModel.isSearchSfwSelected() else viewModel.isSfwSelected())
    }
    var sketchy by remember {
        mutableStateOf(if (useSearchFilters) viewModel.isSearchSketchySelected() else viewModel.isSketchySelected())
    }
    var nsfw by remember {
        mutableStateOf(if (useSearchFilters) viewModel.isSearchNsfwSelected() else viewModel.isNsfwSelected())
    }
    val selectedResolutions = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (if (useSearchFilters) viewModel.searchResolution else viewModel.currentResolution)
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            )
        }
    }
    val selectedRatios = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (if (useSearchFilters) viewModel.searchRatio else viewModel.currentRatio)
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            )
        }
    }
    val selectedColors = remember {
        mutableStateListOf<String>().apply {
            addAll(
                (if (useSearchFilters) viewModel.searchColor else viewModel.currentColor)
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            )
        }
    }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Wallpapers") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text("Categories")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = general,
                        onClick = { general = !general },
                        label = { Text("General") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = anime,
                        onClick = { anime = !anime },
                        label = { Text("Anime") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = people,
                        onClick = { people = !people },
                        label = { Text("People") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Purity")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sfw,
                        onClick = { sfw = !sfw },
                        label = { Text("SFW") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = sketchy,
                        onClick = { sketchy = !sketchy },
                        label = { Text("Sketchy") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = nsfw,
                        onClick = { nsfw = !nsfw },
                        label = { Text("NSFW") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Resolution")
                Spacer(modifier = Modifier.height(8.dp))
                FilterChip(
                    selected = selectedResolutions.isEmpty(),
                    onClick = { selectedResolutions.clear() },
                    label = { Text("Any") },
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                ResolutionGroup(
                    title = "Wide",
                    resolutions = wideResolutionOptions,
                    selectedResolutions = selectedResolutions
                )
                if (portraitResolutionOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ResolutionGroup(
                        title = "Portrait",
                        resolutions = portraitResolutionOptions,
                        selectedResolutions = selectedResolutions
                    )
                }
                if (squareResolutionOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ResolutionGroup(
                        title = "Square",
                        resolutions = squareResolutionOptions,
                        selectedResolutions = selectedResolutions
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Ratio")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRatios.isEmpty(),
                        onClick = { selectedRatios.clear() },
                        label = { Text("Any") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    ratioOptions.filterNot { it == "Any" }.forEach { ratio ->
                        FilterChip(
                            selected = selectedRatios.contains(ratio),
                            onClick = {
                                if (selectedRatios.contains(ratio)) {
                                    selectedRatios.remove(ratio)
                                } else {
                                    selectedRatios.add(ratio)
                                }
                            },
                            label = { Text(ratio) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Color")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedColors.isEmpty(),
                        onClick = { selectedColors.clear() },
                        label = { Text("Any") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    colorOptions.forEach { hex ->
                        val swatchColor = parseColorOrFallback(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(
                                    width = if (selectedColors.contains(hex)) 3.dp else 1.dp,
                                    color = if (selectedColors.contains(hex)) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                                .background(swatchColor, CircleShape)
                                .clickable {
                                    if (selectedColors.contains(hex)) {
                                        selectedColors.remove(hex)
                                    } else {
                                        selectedColors.add(hex)
                                    }
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    general = true
                    anime = true
                    people = true
                    sfw = true
                    sketchy = false
                    nsfw = false
                    selectedResolutions.clear()
                    selectedRatios.clear()
                    selectedColors.clear()
                }) {
                    Text("Reset")
                }
                TextButton(onClick = {
                    val categories = "${if (general) "1" else "0"}${if (anime) "1" else "0"}${if (people) "1" else "0"}"
                    val purity = "${if (sfw) "1" else "0"}${if (sketchy) "1" else "0"}${if (nsfw) "1" else "0"}"
                    if (useSearchFilters) {
                        viewModel.updateSearchFilters(
                            categories = categories,
                            purity = purity,
                            resolution = selectedResolutions.joinToString(","),
                            ratio = selectedRatios.joinToString(","),
                            color = selectedColors.joinToString(",")
                        )
                    } else {
                        viewModel.updateFilters(
                            categories = categories,
                            purity = purity,
                            resolution = selectedResolutions.joinToString(","),
                            ratio = selectedRatios.joinToString(","),
                            color = selectedColors.joinToString(",")
                        )
                    }
                    onDismiss()
                }) { Text("Apply") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun parseColorOrFallback(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (_: IllegalArgumentException) {
        Color.Gray
    }
}

private fun formatResolutionLabel(resolution: String): String {
    return resolution
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResolutionGroup(
    title: String,
    resolutions: List<String>,
    selectedResolutions: MutableList<String>
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        resolutions.forEach { resolution ->
            FilterChip(
                selected = selectedResolutions.contains(resolution),
                onClick = {
                    if (selectedResolutions.contains(resolution)) {
                        selectedResolutions.remove(resolution)
                    } else {
                        selectedResolutions.add(resolution)
                    }
                },
                label = { Text(formatResolutionLabel(resolution)) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

private fun resolutionOrientation(resolution: String): String {
    val parts = resolution.split("x")
    if (parts.size != 2) return "Wide"
    val width = parts[0].toIntOrNull() ?: return "Wide"
    val height = parts[1].toIntOrNull() ?: return "Wide"
    return when {
        width > height -> "Wide"
        width < height -> "Portrait"
        else -> "Square"
    }
}
