package com.example.walls.ui.fullscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.walls.api.WallpaperDetail

@Composable
fun WallpaperInfoSheet(
    details: WallpaperDetail?,
    isLoading: Boolean,
    onTagClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        contentColor = colorScheme.onSurface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                    containerColor = colorScheme.secondaryContainer.copy(alpha = 0.82f),
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
