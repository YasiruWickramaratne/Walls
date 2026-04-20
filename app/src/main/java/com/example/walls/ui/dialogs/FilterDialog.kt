package com.example.walls.ui.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.walls.WallpaperViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    viewModel: WallpaperViewModel,
    useSearchFilters: Boolean,
    onDismiss: () -> Unit
) {
    val initialState = remember(viewModel, useSearchFilters) {
        FilterSelectionState.fromViewModel(viewModel, useSearchFilters)
    }
    var general by remember(initialState) { mutableStateOf(initialState.general) }
    var anime by remember(initialState) { mutableStateOf(initialState.anime) }
    var people by remember(initialState) { mutableStateOf(initialState.people) }
    var sfw by remember(initialState) { mutableStateOf(initialState.sfw) }
    var sketchy by remember(initialState) { mutableStateOf(initialState.sketchy) }
    var nsfw by remember(initialState) { mutableStateOf(initialState.nsfw) }
    val selectedResolutions = remember(initialState) { mutableStateListOf<String>().apply { addAll(initialState.resolutions) } }
    val selectedRatios = remember(initialState) { mutableStateListOf<String>().apply { addAll(initialState.ratios) } }
    val selectedColors = remember(initialState) { mutableStateListOf<String>().apply { addAll(initialState.colors) } }
    val scrollState = rememberScrollState()

    fun applyState(state: FilterSelectionState) {
        if (useSearchFilters) {
            viewModel.updateSearchFilters(
                categories = state.categoriesString,
                purity = state.purityString,
                resolution = state.resolutions.joinToString(","),
                ratio = state.ratios.joinToString(","),
                color = state.colors.joinToString(",")
            )
        } else {
            viewModel.updateFilters(
                categories = state.categoriesString,
                purity = state.purityString,
                resolution = state.resolutions.joinToString(","),
                ratio = state.ratios.joinToString(","),
                color = state.colors.joinToString(",")
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Wallpapers") },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text("Categories")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = general, onClick = { general = !general }, label = { Text("General") }, shape = RoundedCornerShape(16.dp))
                    FilterChip(selected = anime, onClick = { anime = !anime }, label = { Text("Anime") }, shape = RoundedCornerShape(16.dp))
                    FilterChip(selected = people, onClick = { people = !people }, label = { Text("People") }, shape = RoundedCornerShape(16.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Purity")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = sfw, onClick = { sfw = !sfw }, label = { Text("SFW") }, shape = RoundedCornerShape(16.dp))
                    FilterChip(selected = sketchy, onClick = { sketchy = !sketchy }, label = { Text("Sketchy") }, shape = RoundedCornerShape(16.dp))
                    FilterChip(selected = nsfw, onClick = { nsfw = !nsfw }, label = { Text("NSFW") }, shape = RoundedCornerShape(16.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Resolution")
                Spacer(modifier = Modifier.height(8.dp))
                FilterChip(selected = selectedResolutions.isEmpty(), onClick = { selectedResolutions.clear() }, label = { Text("Any") }, shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(12.dp))
                ResolutionGroup(title = "Wide", resolutions = wideResolutionOptions, selectedValues = selectedResolutions)
                if (portraitResolutionOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ResolutionGroup(title = "Portrait", resolutions = portraitResolutionOptions, selectedValues = selectedResolutions)
                }
                if (squareResolutionOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ResolutionGroup(title = "Square", resolutions = squareResolutionOptions, selectedValues = selectedResolutions)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Ratio")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedRatios.isEmpty(), onClick = { selectedRatios.clear() }, label = { Text("Any") }, shape = RoundedCornerShape(16.dp))
                    ratioOptions.filterNot { it == "Any" }.forEach { ratio ->
                        FilterChip(
                            selected = selectedRatios.contains(ratio),
                            onClick = {
                                if (selectedRatios.contains(ratio)) selectedRatios.remove(ratio) else selectedRatios.add(ratio)
                            },
                            label = { Text(ratio) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Color")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = selectedColors.isEmpty(), onClick = { selectedColors.clear() }, label = { Text("Any") }, shape = RoundedCornerShape(16.dp))
                    colorOptions.forEach { hex ->
                        val swatchColor = parseColorOrFallback(hex)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selectedColors.contains(hex)) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clickable {
                                    if (selectedColors.contains(hex)) selectedColors.remove(hex) else selectedColors.add(hex)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .background(swatchColor, CircleShape)
                                    .padding(15.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val defaults = FilterSelectionState.defaults()
                    general = defaults.general
                    anime = defaults.anime
                    people = defaults.people
                    sfw = defaults.sfw
                    sketchy = defaults.sketchy
                    nsfw = defaults.nsfw
                    selectedResolutions.clear()
                    selectedRatios.clear()
                    selectedColors.clear()
                }) {
                    Text("Reset")
                }
                TextButton(onClick = {
                    applyState(
                        FilterSelectionState(
                            general = general,
                            anime = anime,
                            people = people,
                            sfw = sfw,
                            sketchy = sketchy,
                            nsfw = nsfw,
                            resolutions = selectedResolutions.toList(),
                            ratios = selectedRatios.toList(),
                            colors = selectedColors.toList()
                        )
                    )
                    onDismiss()
                }) {
                    Text("Apply")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResolutionGroup(
    title: String,
    resolutions: List<String>,
    selectedValues: MutableList<String>
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        resolutions.forEach { resolution ->
            FilterChip(
                selected = selectedValues.contains(resolution),
                onClick = {
                    if (selectedValues.contains(resolution)) selectedValues.remove(resolution) else selectedValues.add(resolution)
                },
                label = { Text(resolution) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

private fun parseColorOrFallback(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (_: IllegalArgumentException) {
        Color.Gray
    }
}
