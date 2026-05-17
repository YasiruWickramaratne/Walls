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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import com.canhub.cropper.CropImageView
import com.example.walls.ThemeMode
import com.example.walls.WallpaperViewModel
import com.example.walls.api.WallhavenApiService
import com.example.walls.data.analysis.CropCandidateGenerator
import com.example.walls.data.analysis.CropScorerV2
import com.example.walls.data.manager.CropMetadataManager
import com.example.walls.data.manager.ScreenProfileManager
import com.example.walls.data.manager.SmartCropSettingsManager
import com.example.walls.data.model.CollectionStylePreset
import com.example.walls.data.model.CropMetadata
import com.example.walls.data.model.SmartCropMode
import com.example.walls.data.model.SmartCropSettings
import com.example.walls.data.model.WallpaperScreenTarget
import com.example.walls.data.repository.WallpaperRepository
import com.example.walls.ui.components.SmartCropPreviewCard
import com.example.walls.ui.components.SmartCropToggleChip
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
    private var loadedBitmap: Bitmap? = null

    @Inject lateinit var apiService: WallhavenApiService
    @Inject lateinit var wallpaperRepository: WallpaperRepository
    @Inject lateinit var screenProfileManager: ScreenProfileManager
    @Inject lateinit var cropMetadataManager: CropMetadataManager
    @Inject lateinit var cropCandidateGenerator: CropCandidateGenerator
    @Inject lateinit var cropScorerV2: CropScorerV2
    @Inject lateinit var smartCropSettingsManager: SmartCropSettingsManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentWallpaperId = intent.getStringExtra("WALLPAPER_ID")
        currentWallpaperUrl = intent.getStringExtra("IMAGE_URL")
        sourceCollectionName = intent.getStringExtra("COLLECTION_NAME")?.takeIf { it.isNotBlank() }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isAmoled = themeMode == ThemeMode.AMOLED_DARK
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.AMOLED_DARK -> true
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
            val sourceCollection = sourceCollectionName?.let { collectionName ->
                favoriteCollections.firstOrNull { it.name.equals(collectionName, ignoreCase = true) }
            }

            WallsTheme(darkTheme = isDark, amoledDark = isAmoled) {
                val initialSettings = remember { smartCropSettingsManager.loadSettings() }
                var isSmartFit by remember { mutableStateOf(initialSettings.enabled) }
                var selectedTarget by remember { mutableStateOf(initialSettings.previewTarget) }
                var selectedMode by remember {
                    mutableStateOf(
                        initialSettings.mode.takeUnless { it == SmartCropMode.MANUAL || it == SmartCropMode.SMART_FIT }
                            ?: SmartCropMode.AUTO
                    )
                }
                val targetProfile = remember(selectedTarget) { screenProfileManager.getProfile(selectedTarget) }
                var showSmartFitSheet by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Crop Wallpaper") },
                            navigationIcon = {
                                IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                if (isSmartFit) {
                                    TextButton(onClick = { showSmartFitSheet = true }) {
                                        Text("Preview")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        BottomAppBar {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = { cropImageView?.croppedImageAsync() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Crop & Save")
                                }
                                SmartCropToggleChip(
                                    enabled = isSmartFit,
                                    onToggle = { enabled ->
                                        isSmartFit = enabled
                                        showSmartFitSheet = false
                                        persistSmartSettings(
                                            enabled = enabled,
                                            mode = selectedMode,
                                            target = selectedTarget,
                                            separateLockHomeFraming = initialSettings.separateLockHomeFraming
                                        )
                                        if (enabled) {
                                            applySmartCrop(
                                                mode = selectedMode,
                                                target = selectedTarget,
                                                stylePreset = sourceCollection?.stylePreset ?: CollectionStylePreset.DEFAULT
                                            )
                                        } else {
                                            loadSavedCropRect()
                                        }
                                    }
                                )
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

                if (isSmartFit && showSmartFitSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showSmartFitSheet = false }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Smart Fit", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            SmartCropPreviewCard(
                                target = selectedTarget,
                                mode = selectedMode,
                                safeZones = targetProfile.safeZones,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Target",
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WallpaperScreenTarget.entries.forEach { target ->
                                    FilterChip(
                                        selected = selectedTarget == target,
                                        onClick = {
                                            selectedTarget = target
                                            persistSmartSettings(
                                                enabled = isSmartFit,
                                                mode = selectedMode,
                                                target = selectedTarget,
                                                separateLockHomeFraming = initialSettings.separateLockHomeFraming
                                            )
                                            applySmartCrop(
                                                mode = selectedMode,
                                                target = selectedTarget,
                                                stylePreset = sourceCollection?.stylePreset ?: CollectionStylePreset.DEFAULT
                                            )
                                        },
                                        label = { Text(target.name.lowercase().replaceFirstChar { it.titlecase() }) }
                                    )
                                }
                            }
                            Text(
                                "Mode",
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    SmartCropMode.AUTO,
                                    SmartCropMode.SUBJECT_FOCUS,
                                    SmartCropMode.SCENERY,
                                    SmartCropMode.ICON_SAFE,
                                    SmartCropMode.CLOCK_SAFE,
                                    SmartCropMode.DARK_FIT
                                ).forEach { mode ->
                                    FilterChip(
                                        selected = selectedMode == mode,
                                        onClick = {
                                            selectedMode = mode
                                            persistSmartSettings(
                                                enabled = isSmartFit,
                                                mode = selectedMode,
                                                target = selectedTarget,
                                                separateLockHomeFraming = initialSettings.separateLockHomeFraming
                                            )
                                            applySmartCrop(
                                                mode = selectedMode,
                                                target = selectedTarget,
                                                stylePreset = sourceCollection?.stylePreset ?: CollectionStylePreset.DEFAULT
                                            )
                                        },
                                        label = {
                                            Text(mode.name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() })
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
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
                loadedBitmap = bitmap
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

    private fun applySmartCrop(
        mode: SmartCropMode,
        target: WallpaperScreenTarget,
        stylePreset: CollectionStylePreset
    ) {
        val view = cropImageView ?: return
        val id = currentWallpaperId ?: return
        val imageRect = view.wholeImageRect ?: return
        val imageWidth = imageRect.width()
        val imageHeight = imageRect.height()
        if (imageWidth <= 0 || imageHeight <= 0) return

        val profile = screenProfileManager.getProfile(target)
        val candidates = cropCandidateGenerator.generate(imageWidth, imageHeight, profile.aspectRatio)

        lifecycleScope.launch {
            val result = cropScorerV2.bestCrop(
                bitmap = loadedBitmap,
                candidates = candidates,
                mode = mode,
                profile = profile,
                stylePreset = stylePreset
            ) ?: return@launch
            val best = result.rect

            view.cropRect = Rect(
                (best.left * imageWidth).toInt(),
                (best.top * imageHeight).toInt(),
                (best.right * imageWidth).toInt(),
                (best.bottom * imageHeight).toInt()
            )

            cropMetadataManager.save(
                CropMetadata(
                    wallpaperId = id,
                    mode = mode,
                    target = target,
                    leftPercent = best.left,
                    topPercent = best.top,
                    rightPercent = best.right,
                    bottomPercent = best.bottom,
                    score = result.score
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loadedBitmap = null
    }

    private fun persistSmartSettings(
        enabled: Boolean,
        mode: SmartCropMode,
        target: WallpaperScreenTarget,
        separateLockHomeFraming: Boolean
    ) {
        smartCropSettingsManager.saveSettings(
            SmartCropSettings(
                enabled = enabled,
                mode = mode,
                previewTarget = target,
                separateLockHomeFraming = separateLockHomeFraming
            )
        )
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
