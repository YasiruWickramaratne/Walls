package com.example.walls.ui.fullscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.walls.data.repository.FavoriteCollection

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddToCollectionDialog(
    collections: List<FavoriteCollection>,
    wallpaperIds: Set<String>,
    onDismiss: () -> Unit,
    onCreateCollection: (String) -> Unit,
    onToggleCollection: (String) -> Unit
) {
    var newCollectionName by remember { mutableStateOf("") }
    var isCreatingCollection by remember { mutableStateOf(collections.isEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collections") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (collections.isNotEmpty()) {
                    Text(
                        if (wallpaperIds.size > 1) {
                            "Tap a collection to add or remove the selected wallpapers"
                        } else {
                            "Tap a collection to add or remove this wallpaper"
                        }
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        collections.forEach { collection ->
                            val isSelected = wallpaperIds.all { it in collection.wallpaperIds }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleCollection(collection.name) },
                                label = { Text(collection.name) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                if (isCreatingCollection) {
                    Text(if (collections.isEmpty()) "Create a new collection" else "Create another collection")
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        singleLine = true,
                        label = { Text("Collection name") }
                    )
                } else {
                    TextButton(
                        onClick = { isCreatingCollection = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Add new collection")
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingCollection) {
                TextButton(
                    onClick = { onCreateCollection(newCollectionName.trim()) },
                    enabled = newCollectionName.trim().isNotBlank()
                ) {
                    Text("Create")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
