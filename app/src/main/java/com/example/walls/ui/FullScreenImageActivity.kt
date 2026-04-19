package com.example.walls.ui

import android.app.WallpaperManager
import android.graphics.Rect
import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.OnStateChangedListener
import com.example.walls.ThemeMode
import com.example.walls.Wallpaper
import com.example.walls.WallpaperViewModel
import com.example.walls.api.WallpaperDetail
import com.example.walls.ui.theme.WallsTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * SubsamplingScaleImageView that intercepts left/right swipes at minimum zoom
 * and delegates to navigation callbacks instead of panning.
 * Detection uses only displacement (not velocity) so slow or "pause then lift"
 * swipes are still recognised.
 */
private class SwipeableScaleImageView(context: Context) : SubsamplingScaleImageView(context) {
    companion object {
        private const val TAG = "SwipeableScaleImageView"
    }

    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onZoomStateChanged: ((Boolean) -> Unit)? = null

    private var startX = 0f
    private var startY = 0f
    private var isMultiTouch = false
    private var swipeHandled = false
    private var maxDx = 0f
    private var maxDy = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        setOnStateChangedListener(object : OnStateChangedListener {
            override fun onScaleChanged(newScale: Float, origin: Int) {
                val ms = minScale
                val isZoomed = isReady && !ms.isNaN() && ms > 0f && newScale > ms * 1.05f
                onZoomStateChanged?.invoke(isZoomed)
            }

            override fun onCenterChanged(newCenter: android.graphics.PointF?, origin: Int) = Unit
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                isMultiTouch = false
                swipeHandled = false
                maxDx = 0f
                maxDy = 0f
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                isMultiTouch = true
                swipeHandled = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (shouldHandleSwipe(event, source = "move")) {
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (shouldHandleSwipe(event, source = "up")) {
                    return true
                }
                swipeHandled = false
                isMultiTouch = false
            }
            MotionEvent.ACTION_CANCEL -> {
                swipeHandled = false
                isMultiTouch = false
            }
        }
        if (swipeHandled) {
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun shouldHandleSwipe(event: MotionEvent, source: String): Boolean {
        if (isMultiTouch || swipeHandled) return false

        val dx = event.rawX - startX
        val dy = event.rawY - startY
        if (abs(dx) > abs(maxDx)) {
            maxDx = dx
        }
        if (abs(dy) > abs(maxDy)) {
            maxDy = dy
        }
        val ms = minScale
        val notZoomed = !isReady || ms.isNaN() || ms <= 0f || scale <= ms * 1.1f
        // Use the furthest movement seen during the gesture so brief flicks still count
        // even if the last sampled move is short.
        val effectiveDx = maxDx
        val effectiveDy = maxDy
        val minDistancePx = maxOf(8f * resources.displayMetrics.density, touchSlop * 0.75f)
        val clearlyHorizontal = abs(effectiveDx) > minDistancePx && abs(effectiveDx) > abs(effectiveDy) * 1.05f
        val clearlyVertical = abs(effectiveDy) > minDistancePx && abs(effectiveDy) > abs(effectiveDx) * 1.15f

        if (!notZoomed || (!clearlyHorizontal && !clearlyVertical)) {
            Log.d(
                TAG,
                "ignored source=$source dx=$dx dy=$dy effectiveDx=$effectiveDx effectiveDy=$effectiveDy " +
                    "scale=$scale minScale=$minScale isReady=$isReady notZoomed=$notZoomed " +
                    "clearlyHorizontal=$clearlyHorizontal clearlyVertical=$clearlyVertical threshold=$minDistancePx"
            )
            return false
        }

        swipeHandled = true
        parent?.requestDisallowInterceptTouchEvent(true)
        Log.d(
            TAG,
            "accepted source=$source dx=$dx dy=$dy effectiveDx=$effectiveDx effectiveDy=$effectiveDy " +
                "scale=$scale minScale=$minScale direction=${
                    when {
                        clearlyVertical && effectiveDy < 0 -> "up"
                        clearlyVertical -> "down"
                        effectiveDx < 0 -> "left"
                        else -> "right"
                    }
                }"
        )
        when {
            clearlyVertical && effectiveDy < 0 -> onSwipeUp?.invoke()
            clearlyVertical -> onSwipeDown?.invoke()
            effectiveDx < 0 -> onSwipeLeft?.invoke()
            else -> onSwipeRight?.invoke()
        }
        return true
    }
}

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
    val detailsById by viewModel.wallpaperDetails.collectAsStateWithLifecycle()
    val detailsLoading by viewModel.wallpaperDetailsLoading.collectAsStateWithLifecycle()
    val wallpaperList by when (sorting) {
        "date_added" -> viewModel.recentWallpapers.collectAsStateWithLifecycle()
        "toplist" -> viewModel.topWallpapers.collectAsStateWithLifecycle()
        "search" -> viewModel.searchWallpapers.collectAsStateWithLifecycle()
        else -> viewModel.recentWallpapers.collectAsStateWithLifecycle()
    }
    var currentIndex by remember { mutableIntStateOf(startIndex) }
    var isInfoSheetVisible by remember { mutableStateOf(false) }
    var isImageZoomed by remember { mutableStateOf(false) }
    if (wallpaperList.isEmpty()) return
    if (currentIndex > wallpaperList.lastIndex) {
        currentIndex = wallpaperList.lastIndex
    }
    val currentWallpaper = wallpaperList[currentIndex]
    val isFavorite = favorites.contains(currentWallpaper.id)
    val currentDetails = detailsById[currentWallpaper.id]
    val isDetailsLoading = detailsLoading.contains(currentWallpaper.id)

    LaunchedEffect(currentIndex, wallpaperList.size, sorting) {
        val nearEnd = currentIndex >= wallpaperList.lastIndex - 2
        if (nearEnd && viewModel.hasMorePagesForSorting(sorting)) {
            if (sorting == "search") {
                viewModel.fetchSearchWallpapers(searchQuery, isLoadingMore = true)
            } else {
                viewModel.fetchWallpapers(sorting, isLoadingMore = true)
            }
        }
        isInfoSheetVisible = false
        viewModel.fetchWallpaperDetails(currentWallpaper.id)
        wallpaperList.getOrNull(currentIndex + 1)?.id?.let(viewModel::fetchWallpaperDetails)
        wallpaperList.getOrNull(currentIndex - 1)?.id?.let(viewModel::fetchWallpaperDetails)
    }

    LaunchedEffect(isImageZoomed) {
        if (isImageZoomed) {
            isInfoSheetVisible = false
        }
    }

    fun showNextWallpaper() {
        if (currentIndex < wallpaperList.lastIndex) {
            currentIndex++
            if (currentIndex >= wallpaperList.lastIndex - 2 && viewModel.hasMorePagesForSorting(sorting)) {
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
                    IconButton(onClick = { viewModel.toggleFavorite(currentWallpaper.id) }) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite"
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
                targetState = currentIndex,
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
                    onSwipeRight = { if (currentIndex > 0) currentIndex-- },
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
                FloatingActionButton(
                    onClick = { isInfoSheetVisible = true },
                    modifier = Modifier.padding(end = 16.dp, bottom = 28.dp)
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

@Composable
private fun WallpaperInfoSheet(
    details: WallpaperDetail?,
    isLoading: Boolean,
    onTagClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 72.dp.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surface.copy(alpha = 0.94f),
        contentColor = colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .background(colorScheme.onSurface.copy(alpha = 0.25f), CircleShape)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0f || dragOffsetY > 0f) {
                                    dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                if (dragOffsetY >= dismissThresholdPx) {
                                    onDismiss()
                                }
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                dragOffsetY = 0f
                            }
                        )
                    }
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = dragOffsetY }
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = details?.id?.let { "Wallpaper $it" } ?: "Wallpaper details",
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading && details == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading details...", color = colorScheme.onSurface.copy(alpha = 0.75f))
                    return@Column
                }

                if (details == null) {
                    Text("Details are unavailable for this wallpaper.", color = colorScheme.onSurface.copy(alpha = 0.75f))
                    return@Column
                }

                DetailRow("Resolution", details.resolution)
                DetailRow("Dimensions", "${details.dimension_x} x ${details.dimension_y}")
                DetailRow("Type", details.file_type.ifBlank { "Unknown" })
                DetailRow("Size", formatFileSize(details.file_size))
                DetailRow("Category", details.category.replaceFirstChar { it.uppercase() })
                DetailRow("Purity", details.purity.replaceFirstChar { it.uppercase() })
                DetailRow("Views", details.views.toString())
                DetailRow("Favorites", details.favorites.toString())
                DetailRow("Uploaded", details.created_at)

                if (details.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Tags", color = colorScheme.onSurface.copy(alpha = 0.9f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        details.tags.forEach { tag ->
                            AssistChip(
                                onClick = { onTagClick(tag.name) },
                                label = { Text(tag.name) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                    labelColor = colorScheme.onSecondaryContainer.copy(alpha = 0.95f)
                                )
                            )
                        }
                    }
                }

                if (details.colors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Palette", color = colorScheme.onSurface.copy(alpha = 0.9f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        details.colors.take(5).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(parseColorOrFallback(hex), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, color = colorScheme.onSurface)
    }
}

private fun formatFileSize(bytes: Int): String {
    if (bytes <= 0) return "Unknown"
    val mb = bytes / (1024f * 1024f)
    return if (mb >= 1f) String.format("%.1f MB", mb) else String.format("%.0f KB", bytes / 1024f)
}

private fun parseColorOrFallback(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        Color.Gray
    }
}
