package com.example.walls.ui.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.walls.data.repository.FavoriteCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesTopBar(
    mode: FavoritesUiMode,
    onBack: () -> Unit,
    onExitSelection: () -> Unit,
    onDeleteCollection: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                when (mode) {
                    FavoritesUiMode.BrowsingDefault -> "Favorites"
                    is FavoritesUiMode.BrowsingCollection -> mode.name
                    is FavoritesUiMode.Selecting -> "${mode.ids.size} selected"
                }
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(
                onClick = {
                    if (mode is FavoritesUiMode.Selecting) {
                        onExitSelection()
                    } else {
                        onBack()
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (mode is FavoritesUiMode.BrowsingCollection) {
                TextButton(onClick = onDeleteCollection) {
                    Text("Delete")
                }
            }
        }
    )
}

@Composable
fun FavoritesCollectionChips(
    collections: List<FavoriteCollection>,
    selectedCollection: String?,
    onSelectDefault: () -> Unit,
    onSelectCollection: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCollection == null,
            onClick = onSelectDefault,
            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            label = { Text("Default") }
        )
        collections.forEach { collection ->
            FilterChip(
                selected = selectedCollection == collection.name,
                onClick = { onSelectCollection(collection.name) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                label = { Text(collection.name) }
            )
        }
    }
}

@Composable
fun FavoritesSelectionActions(
    visible: Boolean,
    isDefaultSelection: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(visible = visible) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = if (isDefaultSelection) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (isDefaultSelection) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            elevation = FloatingActionButtonDefaults.elevation()
        ) {
            Icon(
                imageVector = if (isDefaultSelection) Icons.Default.Favorite else Icons.Default.Star,
                contentDescription = if (isDefaultSelection) "Unfavorite selected" else "Remove selected from collection"
            )
        }
    }
}

@Composable
fun FavoritesDialogs(
    showBulkRemoveConfirmation: Boolean,
    showDeleteCollectionConfirmation: Boolean,
    showDeleteEmptyCollectionPrompt: Boolean,
    selectedCollection: String?,
    isDefaultSelection: Boolean,
    onDismissBulkRemove: () -> Unit,
    onConfirmBulkRemove: () -> Unit,
    onDismissDeleteCollection: () -> Unit,
    onConfirmDeleteCollection: () -> Unit,
    onDismissDeleteEmptyCollection: () -> Unit,
    onConfirmDeleteEmptyCollection: () -> Unit
) {
    if (showBulkRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissBulkRemove,
            title = {
                Text(
                    if (isDefaultSelection) "Remove selected favorites"
                    else "Remove selected collection items"
                )
            },
            text = {
                Text(
                    if (isDefaultSelection) {
                        "Selected items will be removed from favorites."
                    } else {
                        "Selected items will be removed from this collection."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmBulkRemove) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissBulkRemove) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteCollectionConfirmation && selectedCollection != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteCollection,
            title = { Text("Delete collection") },
            text = { Text("Whole collection will be deleted.") },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteCollection) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteCollection) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteEmptyCollectionPrompt && selectedCollection != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteEmptyCollection,
            title = { Text("Collection is empty") },
            text = { Text("No images in this collection. Shall we delete it?") },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteEmptyCollection) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteEmptyCollection) {
                    Text("Cancel")
                }
            }
        )
    }
}
