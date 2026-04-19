package com.example.walls.ui

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.walls.ThemeMode
import com.example.walls.WallpaperViewModel
import com.example.walls.ui.theme.WallsTheme
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class FullScreenImageActivity : AppCompatActivity() {

    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wallpaperId = intent.getStringExtra("WALLPAPER_ID")
        val imageUrl = intent.getStringExtra("IMAGE_URL")

        if (wallpaperId == null || imageUrl == null) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            finish()
            return
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
                    wallpaperId = wallpaperId,
                    imageUrl = imageUrl,
                    viewModel = viewModel,
                    onSetWallpaper = { flag -> setWallpaper(imageUrl, flag) }
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
    wallpaperId: String,
    imageUrl: String,
    viewModel: WallpaperViewModel,
    onSetWallpaper: (Int) -> Unit
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFavorite = favorites.contains(wallpaperId)

    var imageViewRef by remember { mutableStateOf<SubsamplingScaleImageView?>(null) }

    LaunchedEffect(imageUrl) {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .target { drawable ->
                val bitmap = (drawable as BitmapDrawable).bitmap
                imageViewRef?.setImage(ImageSource.bitmap(bitmap))
            }
            .build()
        context.imageLoader.enqueue(request)
    }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                Row(modifier = Modifier.fillMaxWidth().padding()) {
                    TextButton(
                        onClick = { onSetWallpaper(WallpaperManager.FLAG_SYSTEM) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Home") }
                    TextButton(
                        onClick = { onSetWallpaper(WallpaperManager.FLAG_LOCK) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Lock") }
                    TextButton(
                        onClick = { onSetWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Both") }
                    IconButton(onClick = { viewModel.toggleFavorite(wallpaperId) }) {
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
            AndroidView(
                factory = { ctx ->
                    SubsamplingScaleImageView(ctx).also { imageViewRef = it }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
