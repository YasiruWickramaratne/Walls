package com.example.walls

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WallpaperFragment : Fragment(), FilterUpdateListener {
    private val viewModel: WallpaperViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WallpaperAdapter

    private val isRecentTab: Boolean by lazy { arguments?.getBoolean(ARG_IS_RECENT_TAB) ?: true }
    private val sorting: String by lazy { arguments?.getString(ARG_SORTING) ?: "date_added" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallpaper, container, false)
    }

    override fun onFilterUpdated(categories: String, purity: String) {
        // Log the update for debugging
        Log.d("WallpaperFragment", "Filters updated: categories=$categories, purity=$purity")

        // Refresh the wallpaper list with new filters
        viewModel.fetchWallpapers(sorting)

        // Optionally, you might want to scroll the list back to the top
        recyclerView.scrollToPosition(0)

        // Notify the user that the filters have been updated (optional)
        view?.let {
            Snackbar.make(it, "Filters updated", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WallpaperAdapter { wallpaper ->
            openFullScreenImage(wallpaper)
        }

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            if (isRecentTab) {
                viewModel.recentWallpapers.collectLatest { wallpapers ->
                    Log.d("WallpaperFragment", "Updating recent wallpapers: ${wallpapers.size} items")
                    updateWallpapers(wallpapers)
                }
            } else {
                viewModel.topWallpapers.collectLatest { wallpapers ->
                    Log.d("WallpaperFragment", "Updating top wallpapers: ${wallpapers.size} items")
                    updateWallpapers(wallpapers)
                }
            }
        }
    }

    private fun updateWallpapers(wallpapers: List<Wallpaper>) {
        Log.d("WallpaperFragment", "Submitting ${wallpapers.size} wallpapers to adapter")
        adapter.submitList(wallpapers)
    }

    private fun openFullScreenImage(wallpaper: Wallpaper) {
        val intent = Intent(requireContext(), FullScreenImageActivity::class.java).apply {
            putExtra("WALLPAPER_ID", wallpaper.id)
            putExtra("IMAGE_URL", wallpaper.path)
        }
        startActivity(intent)
    }

    companion object {
        private const val ARG_IS_RECENT_TAB = "is_recent_tab"
        private const val ARG_SORTING = "sorting"
        private const val ARG_CATEGORIES = "categories"
        private const val ARG_PURITY = "purity"

        fun newInstance(
            isRecentTab: Boolean,
            sorting: String,
            categories: String,
            purity: String
        ): WallpaperFragment {
            return WallpaperFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_RECENT_TAB, isRecentTab)
                    putString(ARG_SORTING, sorting)
                    putString(ARG_CATEGORIES, categories)
                    putString(ARG_PURITY, purity)
                }
            }
        }
    }
}