package com.example.walls.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SmartCropToggleChip(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = enabled,
        onClick = { onToggle(!enabled) },
        label = { Text("Smart Fit") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
        },
        modifier = modifier
    )
}
