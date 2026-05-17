package com.example.walls.ui.components

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.walls.data.model.SafeZoneRect
import com.example.walls.data.model.SmartCropMode
import com.example.walls.data.model.WallpaperScreenTarget
import com.example.walls.data.model.WallpaperSafeZones
import kotlin.math.roundToInt

@Composable
fun SmartCropPreviewCard(
    target: WallpaperScreenTarget,
    mode: SmartCropMode,
    safeZones: WallpaperSafeZones,
    bitmap: Bitmap? = null,
    cropRect: RectF? = null,
    score: Float? = null,
    modifier: Modifier = Modifier
) {
    val targetLabel = target.name.lowercase().replaceFirstChar { it.titlecase() }
    val scoreLabel = score?.let { "Score ${(it * 100).roundToInt()}" }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartWallpaperMockup(
                target = target,
                bitmap = bitmap,
                cropRect = cropRect,
                showClock = safeZones.clockZone != null || target == WallpaperScreenTarget.LOCK,
                showIcons = safeZones.iconZone != null || target == WallpaperScreenTarget.HOME,
                modifier = Modifier
                    .height(204.dp)
                    .aspectRatio(9f / 19.5f)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$targetLabel mockup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = modeLabel(mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewPill(targetLabel)
                    scoreLabel?.let { PreviewPill(it) }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun SmartCropPreviewCardPreview() {
    MaterialTheme {
        SmartCropPreviewCard(
            target = WallpaperScreenTarget.BOTH,
            mode = SmartCropMode.ICON_SAFE,
            safeZones = WallpaperSafeZones(
                aspectRatio = 9f / 19.5f,
                clockZone = SafeZoneRect(0.16f, 0.08f, 0.84f, 0.26f),
                iconZone = SafeZoneRect(0.08f, 0.70f, 0.92f, 0.94f)
            ),
            score = 0.78f,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun SmartWallpaperMockup(
    target: WallpaperScreenTarget,
    bitmap: Bitmap?,
    cropRect: RectF?,
    showClock: Boolean,
    showIcons: Boolean,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    val phoneShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .clip(phoneShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = Size(size.width, size.height)
            if (imageBitmap != null && bitmap != null) {
                val src = resolvePreviewSourceRect(
                    bitmap = bitmap,
                    cropRect = cropRect,
                    targetAspectRatio = size.width / size.height
                )
                drawImage(
                    image = imageBitmap,
                    srcOffset = IntOffset(src.left, src.top),
                    srcSize = IntSize(src.width(), src.height()),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                )
            } else {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF20222A), Color(0xFF49615B), Color(0xFF15171C)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    size = canvasSize
                )
            }

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.42f), Color.Transparent, Color.Black.copy(alpha = 0.36f))
                ),
                size = canvasSize
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.28f),
                size = canvasSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        StatusBar(modifier = Modifier.align(Alignment.TopCenter))

        if (showClock || target == WallpaperScreenTarget.BOTH) {
            ClockOverlay(modifier = Modifier.align(Alignment.TopCenter))
        }
        if (showIcons || target == WallpaperScreenTarget.BOTH) {
            HomeOverlay(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun StatusBar(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        drawIntoCanvas {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = 220
                textSize = 8.dp.toPx()
                isAntiAlias = true
            }
            it.nativeCanvas.drawText("9:41", 0f, 8.dp.toPx(), paint)
        }
        drawCircle(Color.White.copy(alpha = 0.85f), radius = 2.dp.toPx(), center = Offset(size.width - 18.dp.toPx(), 5.dp.toPx()))
        drawRoundRect(
            Color.White.copy(alpha = 0.82f),
            topLeft = Offset(size.width - 10.dp.toPx(), 3.dp.toPx()),
            size = Size(10.dp.toPx(), 4.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

@Composable
private fun ClockOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("9:41", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
        Text("Sunday, 17 May", color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HomeOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .then(Modifier)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.72f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(26.dp)
        ) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            val gap = size.width / 5f
            repeat(4) { index ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.76f),
                    radius = 4.dp.toPx(),
                    center = Offset(gap * (index + 1), size.height / 2f)
                )
            }
        }
    }
}

@Composable
private fun PreviewPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun modeLabel(mode: SmartCropMode): String {
    return when (mode) {
        SmartCropMode.CLOCK_SAFE -> "Framed for lock screen readability."
        SmartCropMode.ICON_SAFE -> "Framed to keep app icons readable."
        SmartCropMode.DARK_FIT -> "Prefers a calmer, darker composition."
        SmartCropMode.SCENERY -> "Keeps wide scenery and atmosphere intact."
        SmartCropMode.SUBJECT_FOCUS -> "Keeps the strongest subject in view."
        else -> "Balanced Smart Fit framing."
    }
}

private fun resolvePreviewSourceRect(
    bitmap: Bitmap,
    cropRect: RectF?,
    targetAspectRatio: Float
): Rect {
    val base = cropRect?.let { rect ->
        val left = (rect.left.coerceIn(0f, 1f) * bitmap.width).roundToInt()
        val top = (rect.top.coerceIn(0f, 1f) * bitmap.height).roundToInt()
        val right = (rect.right.coerceIn(0f, 1f) * bitmap.width).roundToInt()
        val bottom = (rect.bottom.coerceIn(0f, 1f) * bitmap.height).roundToInt()
        Rect(
            left.coerceIn(0, bitmap.width - 1),
            top.coerceIn(0, bitmap.height - 1),
            right.coerceIn(left + 1, bitmap.width),
            bottom.coerceIn(top + 1, bitmap.height)
        )
    } ?: Rect(0, 0, bitmap.width, bitmap.height)

    val sourceAspect = base.width().toFloat() / base.height().toFloat()
    if (kotlin.math.abs(sourceAspect - targetAspectRatio) < 0.01f) return base

    return if (sourceAspect > targetAspectRatio) {
        val newWidth = (base.height() * targetAspectRatio).roundToInt().coerceAtLeast(1)
        val left = base.left + ((base.width() - newWidth) / 2)
        Rect(left, base.top, (left + newWidth).coerceAtMost(bitmap.width), base.bottom)
    } else {
        val newHeight = (base.width() / targetAspectRatio).roundToInt().coerceAtLeast(1)
        val top = base.top + ((base.height() - newHeight) / 2)
        Rect(base.left, top, base.right, (top + newHeight).coerceAtMost(bitmap.height))
    }
}
