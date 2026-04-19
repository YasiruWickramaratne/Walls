package com.example.walls.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.walls.ThemeMode
import com.example.walls.WallpaperViewModel
import com.example.walls.worker.AutoWallpaperWorker
import java.util.concurrent.TimeUnit

private val intervalLabels = listOf("15 min", "30 min", "1 hour", "3 hours", "6 hours", "12 hours", "24 hours")
private val intervalMillis = listOf(
    15 * 60 * 1000L, 30 * 60 * 1000L, 60 * 60 * 1000L,
    3 * 60 * 60 * 1000L, 6 * 60 * 60 * 1000L, 12 * 60 * 60 * 1000L, 24 * 60 * 60 * 1000L
)
private val screenLabels = listOf("Home Screen", "Lock Screen", "Both")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: WallpaperViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE) }

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var apiKey by remember { mutableStateOf(prefs.getString("API_KEY", "") ?: "") }
    var apiKeyDirty by remember { mutableStateOf(false) }

    var autoChangeEnabled by remember { mutableStateOf(prefs.getBoolean("AUTO_CHANGE_ENABLED", false)) }

    val savedIntervalMs = prefs.getLong("AUTO_CHANGE_INTERVAL", 15 * 60 * 1000L)
    var selectedIntervalIndex by remember {
        mutableStateOf(intervalMillis.indexOfFirst { it == savedIntervalMs }.coerceAtLeast(0))
    }
    var intervalDropdownExpanded by remember { mutableStateOf(false) }

    var selectedScreenIndex by remember { mutableStateOf(prefs.getInt("WALLPAPER_SCREEN", 0)) }
    var screenDropdownExpanded by remember { mutableStateOf(false) }

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
            // --- Theme Section ---
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

            // --- API Key Section ---
            Text("API Key", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; apiKeyDirty = true },
                label = { Text("Wallhaven API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    prefs.edit().putString("API_KEY", apiKey.trim()).apply()
                    viewModel.saveApiKey(apiKey.trim())
                    apiKeyDirty = false
                },
                enabled = apiKeyDirty,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save API Key")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- Auto Wallpaper Section ---
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
                        val ms = intervalMillis[selectedIntervalIndex]
                        prefs.edit()
                            .putBoolean("AUTO_CHANGE_ENABLED", enabled)
                            .putLong("AUTO_CHANGE_INTERVAL", ms)
                            .putInt("WALLPAPER_SCREEN", selectedScreenIndex)
                            .apply()
                        if (enabled) scheduleAutoWallpaper(context, ms)
                        else cancelAutoWallpaper(context)
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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalDropdownExpanded) },
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
                                val ms = intervalMillis[index]
                                prefs.edit().putLong("AUTO_CHANGE_INTERVAL", ms).apply()
                                if (autoChangeEnabled) {
                                    cancelAutoWallpaper(context)
                                    scheduleAutoWallpaper(context, ms)
                                }
                            }
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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = screenDropdownExpanded) },
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
                                prefs.edit().putInt("WALLPAPER_SCREEN", index).apply()
                                if (autoChangeEnabled) {
                                    cancelAutoWallpaper(context)
                                    scheduleAutoWallpaper(context, intervalMillis[selectedIntervalIndex])
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun scheduleAutoWallpaper(context: Context, intervalMs: Long) {
    val intervalMinutes = intervalMs / (60 * 1000)
    if (intervalMinutes < 5) return
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val request = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(
        intervalMinutes, TimeUnit.MINUTES, 5, TimeUnit.MINUTES
    ).setConstraints(constraints).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "auto_wallpaper_change", ExistingPeriodicWorkPolicy.UPDATE, request
    )
}

private fun cancelAutoWallpaper(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("auto_wallpaper_change")
}
