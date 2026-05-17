package com.example.walls.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import com.example.walls.ui.components.SmartFitPreviewScreen
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
    private var previewBitmap by mutableStateOf<Bitmap?>(null)
    private var smartMockupBitmap by mutableStateOf<Bitmap?>(null)
    private var smartPreviewCropRect by mutableStateOf<RectF?>(null)
    private var latestSmartCropPixelRect: Rect? = null
    private var smartPreviewScore by mutableStateOf<Float?>(null)
    private var smartCropRequestSerial = 0
    private var isApplyingSmartCropProgrammatically = false
    private var pendingCropSaveMode: SmartCropMode? = null

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
                var manualOverridePending by remember { mutableStateOf(false) }

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
                                    onClick = {
                                        if (isSmartFit && manualOverridePending) {
                                            isSmartFit = false
                                            manualOverridePending = false
                                            showSmartFitSheet = false
                                            pendingCropSaveMode = SmartCropMode.MANUAL
                                            latestSmartCropPixelRect = null
                                            smartPreviewCropRect = null
                                            smartMockupBitmap = null
                                            smartPreviewScore = null
                                            persistSmartSettings(
                                                enabled = false,
                                                mode = SmartCropMode.MANUAL,
                                                target = selectedTarget,
                                                separateLockHomeFraming = initialSettings.separateLockHomeFraming
                                            )
                                            Log.d(
                                                "CropActivity",
                                                "SmartFit disabled on manual save crop=${cropImageView?.cropRect}"
                                            )
                                        } else {
                                            pendingCropSaveMode = if (isSmartFit) {
                                                SmartCropMode.SMART_FIT
                                            } else {
                                                SmartCropMode.MANUAL
                                            }
                                        }
                                        cropImageView?.croppedImageAsync()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Crop & Save")
                                }
                                SmartCropToggleChip(
                                    enabled = isSmartFit,
                                    onToggle = { enabled ->
                                        isSmartFit = enabled
                                        manualOverridePending = false
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
                                    view.isAutoZoomEnabled = false
                                    view.setOnCropImageCompleteListener { _, result ->
                                        if (result.isSuccessful) {
                                            Log.d(
                                                "CropActivity",
                                                "SmartFit cropComplete crop=${view.cropRect} window=${view.cropWindowRect} latest=$latestSmartCropPixelRect"
                                            )
                                            Toast.makeText(ctx, "Cropped image saved!", Toast.LENGTH_SHORT).show()
                                            saveCropRect()
                                            pendingCropSaveMode = null
                                        }
                                    }
                                    view.setOnSetCropOverlayReleasedListener { rect ->
                                        if (isSmartFit && !isApplyingSmartCropProgrammatically && rect != null) {
                                            manualOverridePending = true
                                            Log.d(
                                                "CropActivity",
                                                "SmartFit manual override pending rect=$rect previousLatest=$latestSmartCropPixelRect"
                                            )
                                        }
                                    }
                                    view.setOnSetImageUriCompleteListener { _, _, error ->
                                        if (error == null) {
                                            val smartSettings = smartCropSettingsManager.loadSettings()
                                            if (smartSettings.enabled) {
                                                applySmartCrop(
                                                    mode = smartSettings.mode.takeUnless {
                                                        it == SmartCropMode.MANUAL || it == SmartCropMode.SMART_FIT
                                                    } ?: SmartCropMode.AUTO,
                                                    target = smartSettings.previewTarget,
                                                    stylePreset = sourceCollection?.stylePreset ?: CollectionStylePreset.DEFAULT
                                                )
                                            } else {
                                                loadSavedCropRect()
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                    }
                }

                if (isSmartFit && showSmartFitSheet) {
                    SmartFitPreviewScreen(
                        target = selectedTarget,
                        mode = selectedMode,
                        safeZones = targetProfile.safeZones,
                        bitmap = smartMockupBitmap ?: previewBitmap,
                        cropRect = if (smartMockupBitmap != null) null else smartPreviewCropRect,
                        fullBitmap = previewBitmap,
                        fullCropRect = smartPreviewCropRect,
                        score = smartPreviewScore,
                        onTargetChange = { target ->
                            selectedTarget = target
                            persistSmartSettings(
                                enabled = isSmartFit,
                                mode = selectedMode,
                                target = target,
                                separateLockHomeFraming = initialSettings.separateLockHomeFraming
                            )
                            applySmartCrop(
                                mode = selectedMode,
                                target = target,
                                stylePreset = sourceCollection?.stylePreset ?: CollectionStylePreset.DEFAULT
                            )
                        },
                        onModeChange = { mode ->
                            selectedMode = mode
                            persistSmartSettings(
                                enabled = isSmartFit,
                                mode = mode,
                                target = selectedTarget,
                                separateLockHomeFraming = initialSettings.separateLockHomeFraming
                            )
                            applySmartCrop(
                                mode = mode,
                                target = selectedTarget,
                                stylePreset = sourceCollection?.stylePreset ?: CollectionStylePreset.DEFAULT
                            )
                        },
                        onConfirmCrop = {
                            Log.d(
                                "CropActivity",
                                "SmartFit confirm mode=$selectedMode target=$selectedTarget latest=$latestSmartCropPixelRect preview=$smartPreviewCropRect"
                            )
                            showSmartFitSheet = false
                            applyLatestSmartCropToView(saveAfterApply = true)
                        },
                        onDismiss = { showSmartFitSheet = false }
                    )
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
                previewBitmap = bitmap
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
        stylePreset: CollectionStylePreset,
        onApplied: (() -> Unit)? = null
    ) {
        val view = cropImageView ?: return
        val id = currentWallpaperId ?: return
        val imageRect = view.wholeImageRect ?: return
        val imageWidth = imageRect.width()
        val imageHeight = imageRect.height()
        if (imageWidth <= 0 || imageHeight <= 0) return
        val requestSerial = ++smartCropRequestSerial
        Log.d(
            "CropActivity",
            "SmartFit request#$requestSerial mode=$mode target=$target whole=$imageRect loaded=${loadedBitmap?.width}x${loadedBitmap?.height} currentCrop=${view.cropRect} window=${view.cropWindowRect}"
        )

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
            if (requestSerial != smartCropRequestSerial) return@launch
            val best = result.rect
            smartPreviewScore = result.score
            val cropRect = Rect(
                (best.left * imageWidth).toInt(),
                (best.top * imageHeight).toInt(),
                (best.right * imageWidth).toInt(),
                (best.bottom * imageHeight).toInt()
            )
            isApplyingSmartCropProgrammatically = true
            view.cropRect = cropRect
            view.post { isApplyingSmartCropProgrammatically = false }
            latestSmartCropPixelRect = Rect(cropRect)
            smartPreviewCropRect = RectF(
                cropRect.left.toFloat() / imageWidth,
                cropRect.top.toFloat() / imageHeight,
                cropRect.right.toFloat() / imageWidth,
                cropRect.bottom.toFloat() / imageHeight
            )
            smartMockupBitmap = createPreviewBitmapFromCrop(cropRect, imageWidth, imageHeight)
            Log.d(
                "CropActivity",
                "SmartFit applied#$requestSerial mode=$mode target=$target score=${result.score} sourceRect=$cropRect normalized=$smartPreviewCropRect reportedCrop=${view.cropRect} window=${view.cropWindowRect}"
            )
            view.post {
                Log.d(
                    "CropActivity",
                    "SmartFit postApply#$requestSerial mode=$mode target=$target reportedCrop=${view.cropRect} window=${view.cropWindowRect}"
                )
            }

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
            onApplied?.invoke()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loadedBitmap = null
        previewBitmap = null
        smartMockupBitmap = null
        smartPreviewCropRect = null
        latestSmartCropPixelRect = null
        smartPreviewScore = null
    }

    private fun applyLatestSmartCropToView(saveAfterApply: Boolean) {
        val view = cropImageView ?: return
        val rect = latestSmartCropPixelRect
        if (rect == null) {
            Log.w("CropActivity", "SmartFit applyLatest skipped: latestSmartCropPixelRect is null")
            return
        }
        Log.d(
            "CropActivity",
            "SmartFit applyLatest saveAfterApply=$saveAfterApply latest=$rect beforeCrop=${view.cropRect} beforeWindow=${view.cropWindowRect}"
        )
        view.post {
            isApplyingSmartCropProgrammatically = true
            view.cropRect = Rect(rect)
            view.invalidate()
            Log.d(
                "CropActivity",
                "SmartFit applyLatest posted latest=$rect afterCrop=${view.cropRect} afterWindow=${view.cropWindowRect}"
            )
            if (saveAfterApply) {
                view.post {
                    view.cropRect = Rect(rect)
                    view.invalidate()
                    view.post { isApplyingSmartCropProgrammatically = false }
                    view.post {
                        Log.d(
                            "CropActivity",
                            "SmartFit saveLatest latest=$rect cropBeforeSave=${view.cropRect} windowBeforeSave=${view.cropWindowRect}"
                        )
                        pendingCropSaveMode = SmartCropMode.SMART_FIT
                        view.croppedImageAsync()
                    }
                }
            } else {
                view.post { isApplyingSmartCropProgrammatically = false }
            }
        }
    }

    private fun createPreviewBitmapFromCrop(
        cropRect: Rect,
        imageWidth: Int,
        imageHeight: Int
    ): Bitmap? {
        val source = loadedBitmap ?: return null
        if (imageWidth <= 0 || imageHeight <= 0 || source.width <= 0 || source.height <= 0) return null

        val left = (cropRect.left.toFloat() / imageWidth * source.width).toInt().coerceIn(0, source.width - 1)
        val top = (cropRect.top.toFloat() / imageHeight * source.height).toInt().coerceIn(0, source.height - 1)
        val right = (cropRect.right.toFloat() / imageWidth * source.width).toInt().coerceIn(left + 1, source.width)
        val bottom = (cropRect.bottom.toFloat() / imageHeight * source.height).toInt().coerceIn(top + 1, source.height)

        return runCatching {
            Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        }.getOrNull()
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

    private fun saveCropRect(mode: SmartCropMode = pendingCropSaveMode ?: SmartCropMode.MANUAL) {
        val view = cropImageView ?: return
        currentWallpaperId?.let { id ->
            val cropRect = view.cropRect
            val imageWidth = view.wholeImageRect?.width() ?: 0
            val imageHeight = view.wholeImageRect?.height() ?: 0
            if (cropRect != null && imageWidth > 0 && imageHeight > 0) {
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
                cropMetadataManager.save(
                    CropMetadata(
                        wallpaperId = id,
                        mode = mode,
                        target = smartCropSettingsManager.loadSettings().previewTarget,
                        leftPercent = cropRect.left.toFloat() / imageWidth,
                        topPercent = cropRect.top.toFloat() / imageHeight,
                        rightPercent = cropRect.right.toFloat() / imageWidth,
                        bottomPercent = cropRect.bottom.toFloat() / imageHeight,
                        score = 0f
                    )
                )
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
