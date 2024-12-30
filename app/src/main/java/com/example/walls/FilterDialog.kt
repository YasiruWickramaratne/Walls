package com.example.walls

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterDialog : DialogFragment() {

    private val viewModel: WallpaperViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_filter, null)

        // Set dialog title
        builder.setTitle("Filter Wallpapers")

        // Category checkboxes
        val cbGeneral = view.findViewById<CheckBox>(R.id.cb_general)
        val cbAnime = view.findViewById<CheckBox>(R.id.cb_anime)
        val cbPeople = view.findViewById<CheckBox>(R.id.cb_people)
        val cbSelectAllCategories = view.findViewById<CheckBox>(R.id.cb_select_all_categories)

        // Purity checkboxes
        val cbSfw = view.findViewById<CheckBox>(R.id.cb_sfw)
        val cbSketchy = view.findViewById<CheckBox>(R.id.cb_sketchy)
        val cbNsfw = view.findViewById<CheckBox>(R.id.cb_nsfw)
        val cbSelectAllPurity = view.findViewById<CheckBox>(R.id.cb_select_all_purity)

        // Set initial checkbox states
        cbGeneral.isChecked = viewModel.isGeneralSelected()
        cbAnime.isChecked = viewModel.isAnimeSelected()
        cbPeople.isChecked = viewModel.isPeopleSelected()
        cbSfw.isChecked = viewModel.isSfwSelected()
        cbSketchy.isChecked = viewModel.isSketchySelected()
        cbNsfw.isChecked = viewModel.isNsfwSelected()

        // Update "Select All" checkboxes initial state
        cbSelectAllCategories.isChecked = 
            cbGeneral.isChecked && cbAnime.isChecked && cbPeople.isChecked
        cbSelectAllPurity.isChecked = 
            cbSfw.isChecked && cbSketchy.isChecked && cbNsfw.isChecked

        // Set up "Select All" functionality for categories
        cbSelectAllCategories.setOnCheckedChangeListener { _, isChecked ->
            cbGeneral.isChecked = isChecked
            cbAnime.isChecked = isChecked
            cbPeople.isChecked = isChecked
        }

        // Set up "Select All" functionality for purity
        cbSelectAllPurity.setOnCheckedChangeListener { _, isChecked ->
            cbSfw.isChecked = isChecked
            cbSketchy.isChecked = isChecked
            cbNsfw.isChecked = isChecked
        }

        builder.setView(view)
            .setPositiveButton("Apply") { _, _ ->
                val categories = buildCategoriesString(
                    cbGeneral.isChecked,
                    cbAnime.isChecked,
                    cbPeople.isChecked
                )
                val purity = buildPurityString(
                    cbSfw.isChecked,
                    cbSketchy.isChecked,
                    cbNsfw.isChecked
                )
                Log.d("FilterDialog", "Applying filters: categories=$categories, purity=$purity")
                viewModel.updateFilters(categories, purity)
            }
            .setNegativeButton("Cancel", null)

        return builder.create()
    }

    private fun buildCategoriesString(general: Boolean, anime: Boolean, people: Boolean): String {
        return "${if (general) "1" else "0"}${if (anime) "1" else "0"}${if (people) "1" else "0"}"
    }

    private fun buildPurityString(sfw: Boolean, sketchy: Boolean, nsfw: Boolean): String {
        return "${if (sfw) "1" else "0"}${if (sketchy) "1" else "0"}${if (nsfw) "1" else "0"}"
    }
}