package com.example.walls.ui.dialogs

import androidx.compose.runtime.Immutable
import com.example.walls.WallpaperViewModel

@Immutable
data class FilterSelectionState(
    val general: Boolean,
    val anime: Boolean,
    val people: Boolean,
    val sfw: Boolean,
    val sketchy: Boolean,
    val nsfw: Boolean,
    val resolutions: List<String>,
    val ratios: List<String>,
    val colors: List<String>
) {
    val categoriesString: String
        get() = "${if (general) "1" else "0"}${if (anime) "1" else "0"}${if (people) "1" else "0"}"

    val purityString: String
        get() = "${if (sfw) "1" else "0"}${if (sketchy) "1" else "0"}${if (nsfw) "1" else "0"}"

    companion object {
        fun fromViewModel(viewModel: WallpaperViewModel, useSearchFilters: Boolean): FilterSelectionState {
            val resolutionValue = if (useSearchFilters) viewModel.searchResolution else viewModel.currentResolution
            val ratioValue = if (useSearchFilters) viewModel.searchRatio else viewModel.currentRatio
            val colorValue = if (useSearchFilters) viewModel.searchColor else viewModel.currentColor
            return FilterSelectionState(
                general = if (useSearchFilters) viewModel.isSearchGeneralSelected() else viewModel.isGeneralSelected(),
                anime = if (useSearchFilters) viewModel.isSearchAnimeSelected() else viewModel.isAnimeSelected(),
                people = if (useSearchFilters) viewModel.isSearchPeopleSelected() else viewModel.isPeopleSelected(),
                sfw = if (useSearchFilters) viewModel.isSearchSfwSelected() else viewModel.isSfwSelected(),
                sketchy = if (useSearchFilters) viewModel.isSearchSketchySelected() else viewModel.isSketchySelected(),
                nsfw = if (useSearchFilters) viewModel.isSearchNsfwSelected() else viewModel.isNsfwSelected(),
                resolutions = splitFilterValues(resolutionValue),
                ratios = splitFilterValues(ratioValue),
                colors = splitFilterValues(colorValue)
            )
        }

        fun defaults(): FilterSelectionState = FilterSelectionState(
            general = true,
            anime = true,
            people = true,
            sfw = true,
            sketchy = false,
            nsfw = false,
            resolutions = emptyList(),
            ratios = emptyList(),
            colors = emptyList()
        )

        private fun splitFilterValues(value: String): List<String> {
            return value.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
    }
}
