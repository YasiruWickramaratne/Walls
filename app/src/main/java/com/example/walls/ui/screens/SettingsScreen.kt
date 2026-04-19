package com.example.walls.ui.screens

import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.walls.ThemeMode
import com.example.walls.WallpaperViewModel
import com.example.walls.data.manager.AutoWallpaperSettingsManager
import com.example.walls.data.model.AutoWallpaperConfig
import com.example.walls.data.model.AutoWallpaperHistoryEntry
import com.example.walls.data.model.RotationSource
import com.example.walls.data.model.WallpaperScreenTarget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val intervalLabels = listOf("15 min", "30 min", "1 hour", "3 hours", "6 hours", "12 hours", "24 hours")
private val intervalMillis = listOf(
    15 * 60 * 1000L, 30 * 60 * 1000L, 60 * 60 * 1000L,
    3 * 60 * 60 * 1000L, 6 * 60 * 60 * 1000L, 12 * 60 * 60 * 1000L, 24 * 60 * 60 * 1000L
)
private val screenLabels = listOf("Home Screen", "Lock Screen", "Both")
private val rotationSourceLabels = listOf("Favorites only", "Selected collections")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: WallpaperViewModel,
    autoWallpaperSettingsManager: AutoWallpaperSettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val favoriteCollections by viewModel.favoriteCollections.collectAsStateWithLifecycle()
    val initialConfig = remember(autoWallpaperSettingsManager) {
        autoWallpaperSettingsManager.loadConfig()
    }
    val latestHistoryEntry = remember(autoWallpaperSettingsManager) {
        autoWallpaperSettingsManager.loadLatestHistory()
    }

    var apiKey by remember { mutableStateOf(viewModel.getApiKey()) }
    var apiKeyDirty by remember { mutableStateOf(false) }

    var autoChangeEnabled by remember(initialConfig) { mutableStateOf(initialConfig.enabled) }

    var selectedIntervalIndex by remember {
        mutableStateOf(intervalMillis.indexOfFirst { it == initialConfig.intervalMs }.coerceAtLeast(0))
    }
    var intervalDropdownExpanded by remember { mutableStateOf(false) }

    var selectedScreenIndex by remember(initialConfig) {
        mutableStateOf(
            when (initialConfig.screenTarget) {
                WallpaperScreenTarget.HOME -> 0
                WallpaperScreenTarget.LOCK -> 1
                WallpaperScreenTarget.BOTH -> 2
            }
        )
    }
    var screenDropdownExpanded by remember { mutableStateOf(false) }

    var selectedRotationSource by remember(initialConfig) {
        mutableStateOf(initialConfig.rotationSource)
    }
    var rotationSourceDropdownExpanded by remember { mutableStateOf(false) }

    var selectedRotationCollections by remember(initialConfig) {
        mutableStateOf(
            initialConfig.selectedSources
                .toMutableSet()
                .takeIf { it.isNotEmpty() }
                ?: mutableSetOf(AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION)
        )
    }

    fun currentConfig(): AutoWallpaperConfig {
        val screenTarget = when (selectedScreenIndex) {
            1 -> WallpaperScreenTarget.LOCK
            2 -> WallpaperScreenTarget.BOTH
            else -> WallpaperScreenTarget.HOME
        }
        return AutoWallpaperConfig(
            enabled = autoChangeEnabled,
            intervalMs = intervalMillis[selectedIntervalIndex],
            screenTarget = screenTarget,
            rotationSource = selectedRotationSource,
            selectedSources = selectedRotationCollections.toSet()
        )
    }

    fun persistAutoWallpaperSettings() {
        autoWallpaperSettingsManager.saveConfig(currentConfig())
    }

    fun rescheduleIfNeeded() {
        autoWallpaperSettingsManager.scheduleIfEnabled(currentConfig())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Theme", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val themeModes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
            val themeLabels = listOf("Light", "Dark", "System")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeModes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, themeModes.size),
                        label = { Text(themeLabels[index]) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("API Key", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    apiKeyDirty = true
                },
                label = { Text("Wallhaven API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val trimmedApiKey = apiKey.trim()
                    viewModel.saveApiKey(trimmedApiKey)
                    apiKeyDirty = false
                    Toast.makeText(
                        context,
                        if (trimmedApiKey.isBlank()) "API key cleared" else "API key saved",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                enabled = apiKeyDirty,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save API Key")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Auto Wallpaper", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto change wallpaper", modifier = Modifier.weight(1f))
                Switch(
                    checked = autoChangeEnabled,
                    onCheckedChange = { enabled ->
                        autoChangeEnabled = enabled
                        persistAutoWallpaperSettings()
                        if (enabled) {
                            autoWallpaperSettingsManager.scheduleIfEnabled(currentConfig())
                        } else {
                            autoWallpaperSettingsManager.cancel()
                        }
                        Toast.makeText(
                            context,
                            if (enabled) "Auto wallpaper enabled" else "Auto wallpaper disabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = intervalDropdownExpanded,
                onExpandedChange = { intervalDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = intervalLabels[selectedIntervalIndex],
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Change Interval") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalDropdownExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = intervalDropdownExpanded,
                    onDismissRequest = { intervalDropdownExpanded = false }
                ) {
                    intervalLabels.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedIntervalIndex = index
                                intervalDropdownExpanded = false
                                persistAutoWallpaperSettings()
                                rescheduleIfNeeded()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = rotationSourceDropdownExpanded,
                onExpandedChange = { rotationSourceDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (selectedRotationSource == RotationSource.COLLECTIONS) {
                        rotationSourceLabels[1]
                    } else {
                        rotationSourceLabels[0]
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Smart auto-rotation") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = rotationSourceDropdownExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = rotationSourceDropdownExpanded,
                    onDismissRequest = { rotationSourceDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(rotationSourceLabels[0]) },
                        onClick = {
                            selectedRotationSource = RotationSource.FAVORITES
                            rotationSourceDropdownExpanded = false
                            persistAutoWallpaperSettings()
                            rescheduleIfNeeded()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(rotationSourceLabels[1]) },
                        onClick = {
                            selectedRotationSource = RotationSource.COLLECTIONS
                            rotationSourceDropdownExpanded = false
                            if (selectedRotationCollections.isEmpty()) {
                                selectedRotationCollections = mutableSetOf(AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION)
                            }
                            persistAutoWallpaperSettings()
                            rescheduleIfNeeded()
                        }
                    )
                }
            }

            if (selectedRotationSource == RotationSource.COLLECTIONS) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Choose sources", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sourceNames = buildList {
                        add(AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION)
                        addAll(favoriteCollections.map { it.name })
                    }
                    sourceNames.forEach { sourceName ->
                        FilterChip(
                            selected = selectedRotationCollections.contains(sourceName),
                            onClick = {
                                val updated = selectedRotationCollections.toMutableSet()
                                if (updated.contains(sourceName)) {
                                    updated.remove(sourceName)
                                } else {
                                    updated.add(sourceName)
                                }
                                selectedRotationCollections = if (updated.isEmpty()) {
                                    mutableSetOf(AutoWallpaperConfig.DEFAULT_ROTATION_COLLECTION)
                                } else {
                                    updated
                                }
                                persistAutoWallpaperSettings()
                                rescheduleIfNeeded()
                            },
                            label = { Text(sourceName) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = screenDropdownExpanded,
                onExpandedChange = { screenDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = screenLabels[selectedScreenIndex],
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Wallpaper Screen") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = screenDropdownExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = screenDropdownExpanded,
                    onDismissRequest = { screenDropdownExpanded = false }
                ) {
                    screenLabels.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedScreenIndex = index
                                screenDropdownExpanded = false
                                persistAutoWallpaperSettings()
                                rescheduleIfNeeded()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Latest auto changed image", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (latestHistoryEntry == null) {
                Text(
                    text = "No auto wallpaper changes yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AutoWallpaperHistoryRow(latestHistoryEntry)
            }
        }
    }
}

@Composable
private fun AutoWallpaperHistoryRow(entry: AutoWallpaperHistoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.wallpaperName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatHistoryTimestamp(entry.changedAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AsyncImage(
                model = entry.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 38.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

private fun formatHistoryTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
