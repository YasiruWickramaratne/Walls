package com.example.walls.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import com.canhub.cropper.CropImageView
import com.example.walls.ThemeMode
import com.example.walls.WallpaperViewModel
import com.example.walls.api.WallhavenApiService
import com.example.walls.data.repository.WallpaperRepository
import com.example.walls.ui.theme.WallsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class CropActivity : AppCompatActivity() {

    private val viewModel: WallpaperViewModel by viewModels()
    private var currentWallpaperId: String? = null
    private var currentWallpaperUrl: String? = null
    private var sourceCollectionName: String? = null
    private var cropImageView: CropImageView? = null

    @Inject lateinit var apiService: WallhavenApiService
    @Inject lateinit var wallpaperRepository: WallpaperRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentWallpaperId = intent.getStringExtra("WALLPAPER_ID")
        currentWallpaperUrl = intent.getStringExtra("IMAGE_URL")
        sourceCollectionName = intent.getStringExtra("COLLECTION_NAME")?.takeIf { it.isNotBlank() }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val favorites by viewModel.favorites.collectAsStateWithLifecycle()
            val favoriteCollections by viewModel.favoriteCollections.collectAsStateWithLifecycle()
            val isFavorite = favorites.contains(currentWallpaperId)
            val isFromCollection = sourceCollectionName != null
            val isInSourceCollection = sourceCollectionName?.let { collectionName ->
                favoriteCollections.any { collection ->
                    collection.name == collectionName && currentWallpaperId in collection.wallpaperIds
                }
            } ?: false

            WallsTheme(darkTheme = isDark) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Crop Wallpaper") },
                            navigationIcon = {
                                IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        BottomAppBar {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                TextButton(
                                    onClick = { cropImageView?.croppedImageAsync() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Crop & Save") }
                                IconButton(onClick = {
                                    currentWallpaperId?.let { wallpaperId ->
                                        if (isFromCollection) {
                                            sourceCollectionName?.let { collectionName ->
                                                viewModel.toggleWallpaperInCollection(collectionName, wallpaperId)
                                            }
                                        } else {
                                            viewModel.toggleFavorite(wallpaperId)
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (isFromCollection) {
                                            if (isInSourceCollection) Icons.Default.Star else Icons.Default.StarBorder
                                        } else {
                                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                                        },
                                        contentDescription = if (isFromCollection) "Collection" else "Favorite"
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
                                CropImageView(ctx).also { view ->
                                    cropImageView = view
                                    view.setOnCropImageCompleteListener { _, result ->
                                        if (result.isSuccessful) {
                                            Toast.makeText(ctx, "Cropped image saved!", Toast.LENGTH_SHORT).show()
                                            saveCropRect()
                                        }
                                    }
                                    view.setOnSetImageUriCompleteListener { _, _, error ->
                                        if (error == null) loadSavedCropRect()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (!currentWallpaperUrl.isNullOrEmpty()) {
            loadImageToCropView()
        } else {
            loadWallpaperDetails()
        }
    }

    private fun loadWallpaperDetails() {
        lifecycleScope.launch {
            try {
                val apiKey = wallpaperRepository.getApiKey()
                val response = apiService.getWallpaperDetails(currentWallpaperId!!, apiKey)
                currentWallpaperUrl = response.data.path
                loadImageToCropView()
            } catch (e: Exception) {
                Log.e("CropActivity", "Error fetching wallpaper details", e)
                Toast.makeText(this@CropActivity, "Error fetching image details", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadImageToCropView() {
        val url = currentWallpaperUrl ?: return
        lifecycleScope.launch {
            try {
                val dm = resources.displayMetrics
                val request = ImageRequest.Builder(this@CropActivity)
                    .data(url)
                    .allowHardware(false)
                    .size(dm.widthPixels, dm.heightPixels)
                    .build()
                val result = imageLoader.execute(request)
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    ?: run {
                        Toast.makeText(this@CropActivity, "Error loading image", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                val tempFile = withContext(Dispatchers.IO) {
                    File(cacheDir, "crop_preview.jpg").apply {
                        outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                    }
                }
                cropImageView?.setImageUriAsync(Uri.fromFile(tempFile))
            } catch (e: Exception) {
                Log.e("CropActivity", "Error loading image", e)
                Toast.makeText(this@CropActivity, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveCropRect() {
        val view = cropImageView ?: return
        currentWallpaperId?.let { id ->
            val cropRect = view.cropRect
            val imageWidth = view.wholeImageRect?.width() ?: 0
            val imageHeight = view.wholeImageRect?.height() ?: 0
            if (cropRect != null) {
                getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE).edit().apply {
                    putInt("crop_rect_left_$id", cropRect.left)
                    putInt("crop_rect_top_$id", cropRect.top)
                    putInt("crop_rect_right_$id", cropRect.right)
                    putInt("crop_rect_bottom_$id", cropRect.bottom)
                    putFloat("crop_rect_left_percent_$id", cropRect.left.toFloat() / imageWidth)
                    putFloat("crop_rect_top_percent_$id", cropRect.top.toFloat() / imageHeight)
                    putFloat("crop_rect_right_percent_$id", cropRect.right.toFloat() / imageWidth)
                    putFloat("crop_rect_bottom_percent_$id", cropRect.bottom.toFloat() / imageHeight)
                    putInt("original_width_$id", imageWidth)
                    putInt("original_height_$id", imageHeight)
                    apply()
                }
            }
        }
    }

    private fun loadSavedCropRect() {
        val view = cropImageView ?: return
        currentWallpaperId?.let { id ->
            val prefs = getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)
            val left = prefs.getInt("crop_rect_left_$id", 0)
            val top = prefs.getInt("crop_rect_top_$id", 0)
            val right = prefs.getInt("crop_rect_right_$id", 0)
            val bottom = prefs.getInt("crop_rect_bottom_$id", 0)
            if (left != 0 || top != 0 || right != 0 || bottom != 0) {
                view.cropRect = Rect(left, top, right, bottom)
            }
        }
    }
}
