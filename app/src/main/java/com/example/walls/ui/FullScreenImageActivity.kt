package com.example.walls.ui

import android.app.WallpaperManager
import android.graphics.Rect
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import com.davemorrissey.labs.subscaleview.ImageSource
import com.example.walls.ThemeMode
import com.example.walls.Wallpaper
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.fullscreen.SwipeableScaleImageView
import com.example.walls.ui.fullscreen.rememberFullscreenNavigationState
import com.example.walls.ui.fullscreen.components.AddToCollectionDialog
import com.example.walls.ui.fullscreen.components.WallpaperInfoSheet
import com.example.walls.ui.theme.WallsTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.material3.Text

@AndroidEntryPoint
class FullScreenImageActivity : AppCompatActivity() {

    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startIndex = intent.getIntExtra("WALLPAPER_INDEX", 0)
        val initialWallpaperList: List<Wallpaper> = intent.getStringExtra("WALLPAPER_LIST")
            ?.let { json ->
                try {
                    val type = object : TypeToken<List<Wallpaper>>() {}.type
                    Gson().fromJson(json, type)
                } catch (e: Exception) { null }
            }
            ?: run {
                val id = intent.getStringExtra("WALLPAPER_ID") ?: return@run emptyList()
                val url = intent.getStringExtra("IMAGE_URL") ?: return@run emptyList()
                listOf(Wallpaper(id, url, url, com.example.walls.Thumbs(url, url, url)))
            }
        val sorting = intent.getStringExtra("WALLPAPER_SORTING") ?: "date_added"
        val searchQuery = intent.getStringExtra("WALLPAPER_SEARCH_QUERY").orEmpty()
        val nextPage = intent.getIntExtra("WALLPAPER_NEXT_PAGE", 2)
        val hasMorePages = intent.getBooleanExtra("WALLPAPER_HAS_MORE", true)

        if (initialWallpaperList.isEmpty()) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.seedWallpapersForSorting(
            sorting = sorting,
            wallpapers = initialWallpaperList,
            nextPage = nextPage,
            hasMorePages = hasMorePages
        )
        if (sorting == "search" && searchQuery.isNotBlank()) {
            viewModel.setCurrentSearchQuery(searchQuery)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            WallsTheme(darkTheme = isDark) {
                FullScreenImageScreen(
                    sorting = sorting,
                    searchQuery = searchQuery,
                    startIndex = startIndex,
                    viewModel = viewModel,
                    onSetWallpaper = { imageUrl, flag -> setWallpaper(imageUrl, flag) }
                )
            }
        }
    }

    private fun setWallpaper(imageUrl: String, flag: Int) {
        val context = this
        lifecycleScope.launch {
            try {
                val bitmap: Bitmap = withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    (result.drawable as BitmapDrawable).bitmap
                }
                withContext(Dispatchers.IO) {
                    WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, flag)
                }
                Toast.makeText(context, "Wallpaper set successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun FullScreenImageScreen(
    sorting: String,
    searchQuery: String,
    startIndex: Int,
    viewModel: WallpaperViewModel,
    onSetWallpaper: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteCollections by viewModel.favoriteCollections.collectAsStateWithLifecycle()
    val detailsById by viewModel.wallpaperDetails.collectAsStateWithLifecycle()
    val detailsLoading by viewModel.wallpaperDetailsLoading.collectAsStateWithLifecycle()
    val wallpaperList by when (sorting) {
        "date_added" -> viewModel.recentWallpapers.collectAsStateWithLifecycle()
        "toplist" -> viewModel.topWallpapers.collectAsStateWithLifecycle()
        "search" -> viewModel.searchWallpapers.collectAsStateWithLifecycle()
        else -> viewModel.recentWallpapers.collectAsStateWithLifecycle()
    }
    val navigationState = rememberFullscreenNavigationState(startIndex)
    var isInfoSheetVisible by remember { mutableStateOf(false) }
    var isImageZoomed by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    if (wallpaperList.isEmpty()) return
    navigationState.syncToBounds(wallpaperList.lastIndex)
    val currentWallpaper = wallpaperList[navigationState.currentIndex]
    val isFavorite = favorites.contains(currentWallpaper.id)
    val isInCollection = favoriteCollections.any { currentWallpaper.id in it.wallpaperIds }
    val currentDetails = detailsById[currentWallpaper.id]
    val isDetailsLoading = detailsLoading.contains(currentWallpaper.id)

    LaunchedEffect(navigationState.currentIndex, wallpaperList.size, sorting) {
        val nearEnd = navigationState.currentIndex >= wallpaperList.lastIndex - 2
        if (nearEnd && viewModel.hasMorePagesForSorting(sorting)) {
            if (sorting == "search") {
                viewModel.fetchSearchWallpapers(searchQuery, isLoadingMore = true)
            } else {
                viewModel.fetchWallpapers(sorting, isLoadingMore = true)
            }
        }
        isInfoSheetVisible = false
        viewModel.fetchWallpaperDetails(currentWallpaper.id)
        wallpaperList.getOrNull(navigationState.currentIndex + 1)?.id?.let(viewModel::fetchWallpaperDetails)
        wallpaperList.getOrNull(navigationState.currentIndex - 1)?.id?.let(viewModel::fetchWallpaperDetails)
    }

    LaunchedEffect(isImageZoomed) {
        if (isImageZoomed) {
            isInfoSheetVisible = false
        }
    }

    fun showNextWallpaper() {
        if (navigationState.showNext(wallpaperList.lastIndex)) {
            if (navigationState.currentIndex >= wallpaperList.lastIndex - 2 && viewModel.hasMorePagesForSorting(sorting)) {
                if (sorting == "search") {
                    viewModel.fetchSearchWallpapers(searchQuery, isLoadingMore = true)
                } else {
                    viewModel.fetchWallpapers(sorting, isLoadingMore = true)
                }
            }
        } else if (viewModel.hasMorePagesForSorting(sorting)) {
            if (sorting == "search") {
                viewModel.fetchSearchWallpapers(searchQuery, isLoadingMore = true)
            } else {
                viewModel.fetchWallpapers(sorting, isLoadingMore = true)
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { onSetWallpaper(currentWallpaper.path, WallpaperManager.FLAG_SYSTEM) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Home") }
                    TextButton(
                        onClick = { onSetWallpaper(currentWallpaper.path, WallpaperManager.FLAG_LOCK) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Lock") }
                    TextButton(
                        onClick = { onSetWallpaper(currentWallpaper.path, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Both") }
                    IconButton(onClick = { showCollectionDialog = true }) {
                        Icon(
                            if (isInCollection) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Collections"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = navigationState.currentIndex,
                transitionSpec = {
                    val slideFraction = 0.18f
                    val slideDistance = { fullWidth: Int -> (fullWidth * slideFraction).roundToInt() }
                    if (targetState > initialState) {
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            initialOffsetX = { slideDistance(it) }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 160)
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            targetOffsetX = { -slideDistance(it) }
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 140)
                        )
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            initialOffsetX = { -slideDistance(it) }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 160)
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            targetOffsetX = { slideDistance(it) }
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 140)
                        )
                    }
                },
                label = "wallpaper",
                modifier = Modifier.fillMaxSize()
            ) { index ->
                WallpaperPageView(
                    wallpaper = wallpaperList[index],
                    onSwipeLeft = { showNextWallpaper() },
                    onSwipeRight = { navigationState.showPrevious() },
                    onSwipeUp = { if (!isImageZoomed) isInfoSheetVisible = true },
                    onSwipeDown = { isInfoSheetVisible = false },
                    onZoomStateChanged = { zoomed -> isImageZoomed = zoomed }
                )
            }

            AnimatedVisibility(
                visible = isInfoSheetVisible && !isImageZoomed,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(animationSpec = tween(180)),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    targetOffsetY = { it / 2 }
                ) + fadeOut(animationSpec = tween(120)),
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clickable { isInfoSheetVisible = false }
                    )
                    WallpaperInfoSheet(
                        details = currentDetails,
                        isLoading = isDetailsLoading,
                        onTagClick = { tagName ->
                            context.startActivity(SearchActivity.createIntent(context, tagName))
                        },
                        onDismiss = { isInfoSheetVisible = false },
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = !isInfoSheetVisible && !isImageZoomed,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(120)),
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopStart)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        val activity = context as? AppCompatActivity
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    },
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }

            AnimatedVisibility(
                visible = !isInfoSheetVisible && !isImageZoomed,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(120)),
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd)
            ) {
                Column(
                    modifier = Modifier.padding(end = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.End
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.toggleFavorite(currentWallpaper.id) }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite"
                        )
                    }
                    FloatingActionButton(
                        onClick = { isInfoSheetVisible = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Show wallpaper details"
                        )
                    }
                }
            }
        }
    }

    if (showCollectionDialog) {
        AddToCollectionDialog(
            collections = favoriteCollections,
            wallpaperIds = setOf(currentWallpaper.id),
            onDismiss = { showCollectionDialog = false },
            onCreateCollection = { name ->
                val created = viewModel.createFavoriteCollection(name)
                if (created) {
                    viewModel.addWallpaperToCollection(name, currentWallpaper.id)
                    Toast.makeText(context, "Added to $name", Toast.LENGTH_SHORT).show()
                    showCollectionDialog = false
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
                val added = viewModel.toggleWallpaperInCollection(name, currentWallpaper.id)
                Toast.makeText(
                    context,
                    if (added) "Added to $name" else "Removed from $name",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun WallpaperPageView(
    wallpaper: Wallpaper,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val imageViewRef = remember { mutableStateOf<SwipeableScaleImageView?>(null) }

    DisposableEffect(wallpaper.path) {
        val request = ImageRequest.Builder(context)
            .data(wallpaper.path)
            .allowHardware(false)
            .target { drawable ->
                val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return@target
                imageViewRef.value?.setImage(ImageSource.bitmap(bitmap))
            }
            .build()
        val disposable = context.imageLoader.enqueue(request)
        onDispose { disposable.dispose() }
    }

    AndroidView(
        factory = { ctx ->
            SwipeableScaleImageView(ctx).also { imageViewRef.value = it }
        },
        update = { view ->
            view.onSwipeLeft = onSwipeLeft
            view.onSwipeRight = onSwipeRight
            view.onSwipeUp = onSwipeUp
            view.onSwipeDown = onSwipeDown
            view.onZoomStateChanged = onZoomStateChanged
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.doOnLayout { laidOutView ->
                    val edgeWidth = (48 * laidOutView.resources.displayMetrics.density).toInt()
                    laidOutView.systemGestureExclusionRects = listOf(
                        Rect(0, 0, edgeWidth, laidOutView.height),
                        Rect(laidOutView.width - edgeWidth, 0, laidOutView.width, laidOutView.height)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
