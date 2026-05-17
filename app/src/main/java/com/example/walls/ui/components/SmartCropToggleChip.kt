package com.example.walls.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SmartCropToggleChip(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val stateModifier = modifier.semantics {
        role = Role.Switch
        selected = enabled
    }

    if (enabled) {
        FilledTonalButton(
            onClick = { onToggle(false) },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = stateModifier
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Text("Smart Fit On")
        }
    } else {
        OutlinedButton(
            onClick = { onToggle(true) },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = stateModifier
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
            Text("Smart Fit Off")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartCropToggleChipPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmartCropToggleChip(enabled = true, onToggle = {})
            SmartCropToggleChip(enabled = false, onToggle = {})
        }
    }
}
