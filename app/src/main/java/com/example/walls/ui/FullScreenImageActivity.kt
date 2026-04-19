package com.example.walls.ui

import android.app.WallpaperManager
import android.graphics.Rect
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.walls.ThemeMode
import com.example.walls.Wallpaper
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.theme.WallsTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

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

    private var startX = 0f
    private var startY = 0f
    private var isMultiTouch = false
    private var swipeHandled = false
    private var maxDx = 0f
    private var maxDy = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

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

        if (!notZoomed || !clearlyHorizontal) {
            Log.d(
                TAG,
                "ignored source=$source dx=$dx dy=$dy effectiveDx=$effectiveDx effectiveDy=$effectiveDy " +
                    "scale=$scale minScale=$minScale isReady=$isReady notZoomed=$notZoomed " +
                    "clearlyHorizontal=$clearlyHorizontal threshold=$minDistancePx"
            )
            return false
        }

        swipeHandled = true
        parent?.requestDisallowInterceptTouchEvent(true)
        Log.d(
            TAG,
            "accepted source=$source dx=$dx dy=$dy effectiveDx=$effectiveDx effectiveDy=$effectiveDy " +
                "scale=$scale minScale=$minScale direction=${if (effectiveDx < 0) "left" else "right"}"
        )
        if (effectiveDx < 0) onSwipeLeft?.invoke() else onSwipeRight?.invoke()
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
    startIndex: Int,
    viewModel: WallpaperViewModel,
    onSetWallpaper: (String, Int) -> Unit
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val wallpaperList by if (sorting == "date_added") {
        viewModel.recentWallpapers.collectAsStateWithLifecycle()
    } else {
        viewModel.topWallpapers.collectAsStateWithLifecycle()
    }
    var currentIndex by remember { mutableIntStateOf(startIndex) }
    if (wallpaperList.isEmpty()) return
    if (currentIndex > wallpaperList.lastIndex) {
        currentIndex = wallpaperList.lastIndex
    }
    val currentWallpaper = wallpaperList[currentIndex]
    val isFavorite = favorites.contains(currentWallpaper.id)

    LaunchedEffect(currentIndex, wallpaperList.size, sorting) {
        val nearEnd = currentIndex >= wallpaperList.lastIndex - 2
        if (nearEnd && viewModel.hasMorePagesForSorting(sorting)) {
            viewModel.fetchWallpapers(sorting, isLoadingMore = true)
        }
    }

    fun showNextWallpaper() {
        if (currentIndex < wallpaperList.lastIndex) {
            currentIndex++
            if (currentIndex >= wallpaperList.lastIndex - 2 && viewModel.hasMorePagesForSorting(sorting)) {
                viewModel.fetchWallpapers(sorting, isLoadingMore = true)
            }
        } else if (viewModel.hasMorePagesForSorting(sorting)) {
            viewModel.fetchWallpapers(sorting, isLoadingMore = true)
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
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "wallpaper",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { index ->
            WallpaperPageView(
                wallpaper = wallpaperList[index],
                onSwipeLeft = { showNextWallpaper() },
                onSwipeRight = { if (currentIndex > 0) currentIndex-- }
            )
        }
    }
}

@Composable
private fun WallpaperPageView(
    wallpaper: Wallpaper,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
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
