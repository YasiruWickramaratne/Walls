package com.example.walls.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.walls.data.model.SmartCropMode
import com.example.walls.data.model.WallpaperScreenTarget
import com.example.walls.data.model.WallpaperSafeZones
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFitPreviewScreen(
    target: WallpaperScreenTarget,
    mode: SmartCropMode,
    safeZones: WallpaperSafeZones,
    bitmap: Bitmap?,
    cropRect: RectF?,
    fullBitmap: Bitmap?,
    fullCropRect: RectF?,
    score: Float?,
    onTargetChange: (WallpaperScreenTarget) -> Unit,
    onModeChange: (SmartCropMode) -> Unit,
    onConfirmCrop: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            SmartFitPreviewContent(
                target = target,
                mode = mode,
                safeZones = safeZones,
                bitmap = bitmap,
                cropRect = cropRect,
                fullBitmap = fullBitmap,
                fullCropRect = fullCropRect,
                score = score,
                onTargetChange = onTargetChange,
                onModeChange = onModeChange,
                onConfirmCrop = onConfirmCrop,
                onDismiss = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartFitPreviewContent(
    target: WallpaperScreenTarget,
    mode: SmartCropMode,
    safeZones: WallpaperSafeZones,
    bitmap: Bitmap?,
    cropRect: RectF?,
    fullBitmap: Bitmap?,
    fullCropRect: RectF?,
    score: Float?,
    onTargetChange: (WallpaperScreenTarget) -> Unit,
    onModeChange: (SmartCropMode) -> Unit,
    onConfirmCrop: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Fit Preview") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = 132.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SmartWallpaperMockup(
                    target = target,
                    bitmap = bitmap,
                    cropRect = cropRect,
                    showClock = safeZones.clockZone != null || target == WallpaperScreenTarget.LOCK,
                    showIcons = safeZones.iconZone != null || target == WallpaperScreenTarget.HOME,
                    modifier = Modifier
                        .height(420.dp)
                        .aspectRatio(9f / 19.5f)
                )

                FramingMap(
                    bitmap = fullBitmap ?: bitmap,
                    cropRect = fullCropRect ?: cropRect,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${target.label()} mockup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = mode.description(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    score?.let {
                        Text(
                            text = "Smart Fit score ${(it * 100).roundToInt()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                ControlGroup(title = "Target") {
                    WallpaperScreenTarget.entries.forEach { option ->
                        FilterChip(
                            selected = target == option,
                            onClick = { onTargetChange(option) },
                            label = { Text(option.label()) }
                        )
                    }
                }

                ControlGroup(title = "Mode") {
                    previewModes.forEach { option ->
                        FilterChip(
                            selected = mode == option,
                            onClick = { onModeChange(option) },
                            label = { Text(option.label()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
            FloatingActionButton(
                onClick = onConfirmCrop,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(end = 20.dp, bottom = 72.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Crop and save")
            }
        }
    }
}

@Composable
private fun FramingMap(
    bitmap: Bitmap?,
    cropRect: RectF?,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Framing", style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (imageBitmap != null && bitmap != null) {
                    drawImage(
                        image = imageBitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bitmap.width, bitmap.height),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                    )
                } else {
                    drawRect(Color(0xFF252028), size = Size(size.width, size.height))
                }

                drawRect(Color.Black.copy(alpha = 0.28f), size = Size(size.width, size.height))

                val rect = cropRect ?: RectF(0f, 0f, 1f, 1f)
                val left = rect.left.coerceIn(0f, 1f) * size.width
                val top = rect.top.coerceIn(0f, 1f) * size.height
                val right = rect.right.coerceIn(0f, 1f) * size.width
                val bottom = rect.bottom.coerceIn(0f, 1f) * size.height

                drawRect(
                    color = Color.Black.copy(alpha = 0.48f),
                    topLeft = Offset.Zero,
                    size = Size(size.width, top)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.48f),
                    topLeft = Offset(0f, bottom),
                    size = Size(size.width, size.height - bottom)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.48f),
                    topLeft = Offset.Zero,
                    size = Size(left, size.height)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.48f),
                    topLeft = Offset(right, 0f),
                    size = Size(size.width - right, size.height)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(left, top),
                    size = Size((right - left).coerceAtLeast(1f), (bottom - top).coerceAtLeast(1f)),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun ControlGroup(
    title: String,
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

private val previewModes = listOf(
    SmartCropMode.AUTO,
    SmartCropMode.SUBJECT_FOCUS,
    SmartCropMode.SCENERY,
    SmartCropMode.ICON_SAFE,
    SmartCropMode.CLOCK_SAFE,
    SmartCropMode.DARK_FIT
)

private fun WallpaperScreenTarget.label(): String {
    return name.lowercase().replaceFirstChar { it.titlecase() }
}

private fun SmartCropMode.label(): String {
    return name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
}

private fun SmartCropMode.description(): String {
    return when (this) {
        SmartCropMode.CLOCK_SAFE -> "Framed for lock screen readability."
        SmartCropMode.ICON_SAFE -> "Framed to keep app icons readable."
        SmartCropMode.DARK_FIT -> "Prefers a calmer, darker composition."
        SmartCropMode.SCENERY -> "Keeps wide scenery and atmosphere intact."
        SmartCropMode.SUBJECT_FOCUS -> "Keeps the strongest subject in view."
        else -> "Balanced Smart Fit framing."
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SmartFitPreviewScreenPreview() {
    val previewBitmap = remember {
        Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(74, 92, 88))
        }
    }

    MaterialTheme {
        SmartFitPreviewContent(
            target = WallpaperScreenTarget.HOME,
            mode = SmartCropMode.ICON_SAFE,
            safeZones = WallpaperSafeZones(
                aspectRatio = 9f / 19.5f,
                iconZone = com.example.walls.data.model.SafeZoneRect(0.08f, 0.70f, 0.92f, 0.94f)
            ),
            bitmap = previewBitmap,
            cropRect = null,
            fullBitmap = previewBitmap,
            fullCropRect = RectF(0.28f, 0f, 0.72f, 1f),
            score = 0.82f,
            onTargetChange = {},
            onModeChange = {},
            onConfirmCrop = {},
            onDismiss = {}
        )
    }
}
