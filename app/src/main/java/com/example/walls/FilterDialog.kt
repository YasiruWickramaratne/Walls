import android.app.Dialog
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.walls.R
import com.example.walls.WallpaperViewModel

class FilterDialog(private val viewModel: WallpaperViewModel) : DialogFragment() {

    private var onFilterApplied: ((categories: String, purity: String) -> Unit)? = null

    fun setOnFilterAppliedListener(listener: (categories: String, purity: String) -> Unit) {
        this.onFilterApplied = listener
    }

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

        // Set initial checkbox states, restoring from savedInstanceState if available
        cbGeneral.isChecked = savedInstanceState?.getBoolean("general", viewModel.isGeneralSelected()) ?: viewModel.isGeneralSelected()
        cbAnime.isChecked = savedInstanceState?.getBoolean("anime", viewModel.isAnimeSelected()) ?: viewModel.isAnimeSelected()
        cbPeople.isChecked = savedInstanceState?.getBoolean("people", viewModel.isPeopleSelected()) ?: viewModel.isPeopleSelected()

        cbSfw.isChecked = savedInstanceState?.getBoolean("sfw", viewModel.isSfwSelected()) ?: viewModel.isSfwSelected()
        cbSketchy.isChecked = savedInstanceState?.getBoolean("sketchy", viewModel.isSketchySelected()) ?: viewModel.isSketchySelected()
        cbNsfw.isChecked = savedInstanceState?.getBoolean("nsfw", viewModel.isNsfwSelected()) ?: viewModel.isNsfwSelected()


        // Update "Select All" checkboxes
        cbSelectAllCategories.isChecked =
            cbGeneral.isChecked && cbAnime.isChecked && cbPeople.isChecked
        cbSelectAllPurity.isChecked = cbSfw.isChecked && cbSketchy.isChecked && cbNsfw.isChecked

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
                val purity =
                    buildPurityString(cbSfw.isChecked, cbSketchy.isChecked, cbNsfw.isChecked)
                onFilterApplied?.invoke(categories, purity)
            }
            .setNegativeButton("Cancel", null)

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val view = dialog?.window?.decorView
        view?.let {
            outState.putBoolean("general", it.findViewById<CheckBox>(R.id.cb_general).isChecked)
            outState.putBoolean("anime", it.findViewById<CheckBox>(R.id.cb_anime).isChecked)
            outState.putBoolean("people", it.findViewById<CheckBox>(R.id.cb_people).isChecked)
            outState.putBoolean("sfw", it.findViewById<CheckBox>(R.id.cb_sfw).isChecked)
            outState.putBoolean("sketchy", it.findViewById<CheckBox>(R.id.cb_sketchy).isChecked)
            outState.putBoolean("nsfw", it.findViewById<CheckBox>(R.id.cb_nsfw).isChecked)
        }
    }

    private fun buildCategoriesString(general: Boolean, anime: Boolean, people: Boolean): String {
        return "${if (general) "1" else "0"}${if (anime) "1" else "0"}${if (people) "1" else "0"}"
    }

    private fun buildPurityString(sfw: Boolean, sketchy: Boolean, nsfw: Boolean): String {
        return "${if (sfw) "1" else "0"}${if (sketchy) "1" else "0"}${if (nsfw) "1" else "0"}"
    }
}