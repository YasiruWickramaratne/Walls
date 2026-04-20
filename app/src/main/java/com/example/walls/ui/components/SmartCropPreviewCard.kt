package com.example.walls.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.walls.data.model.SmartCropMode
import com.example.walls.data.model.WallpaperScreenTarget
import com.example.walls.data.model.WallpaperSafeZones

@Composable
fun SmartCropPreviewCard(
    target: WallpaperScreenTarget,
    mode: SmartCropMode,
    safeZones: WallpaperSafeZones,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${target.name.lowercase().replaceFirstChar { it.titlecase() }} preview",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = when (mode) {
                    SmartCropMode.CLOCK_SAFE -> "Keeps the clock area clearer."
                    SmartCropMode.ICON_SAFE -> "Keeps the icon area calmer."
                    SmartCropMode.DARK_FIT -> "Biases toward darker composition."
                    SmartCropMode.SCENERY -> "Biases toward open framing."
                    SmartCropMode.SUBJECT_FOCUS -> "Biases toward strong subjects."
                    else -> "Balanced Smart Fit preview."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(148.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                safeZones.clockZone?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                    )
                }
                safeZones.iconZone?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .padding(top = 92.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp))
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                safeZones.clockZone?.let {
                    Text("Clock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                safeZones.iconZone?.let {
                    Text("Icons", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
