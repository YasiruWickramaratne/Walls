package com.example.walls

import WallpaperAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WallpaperFragment : Fragment() {
    private lateinit var viewModel: WallpaperViewModel
    private lateinit var adapter: WallpaperAdapter
    private lateinit var recyclerView: RecyclerView
    private val isRecentTab: Boolean by lazy { arguments?.getBoolean(ARG_IS_RECENT_TAB) ?: true }
private val sorting: String by lazy { arguments?.getString(ARG_SORTING) ?: "date_added" }
    private var categories: String = "111"
    private var purity: String = "100"


override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    Log.d("WallpaperFragment", "onCreateView called for ${if (isRecentTab) "Recent" else "Top"} tab")
    return inflater.inflate(R.layout.fragment_wallpaper, container, false)
}

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Log.d("WallpaperFragment", "onViewCreated called for ${if (isRecentTab) "Recent" else "Top"} tab")

    viewModel = ViewModelProvider(requireActivity())[WallpaperViewModel::class.java]
    adapter = WallpaperAdapter()

    // Set up RecyclerView
    recyclerView = view.findViewById(R.id.recyclerView)
    recyclerView.layoutManager = GridLayoutManager(context, 2)
    recyclerView.adapter = adapter

    // Observe wallpapers
    observeWallpapers()

    // Observe filter changes
    viewModel.filterChanged.observe(viewLifecycleOwner) { changed ->
        if (changed) {
            Log.d("WallpaperFragment", "Filter changed, observing wallpapers")
            categories = viewModel.currentCategories
            purity = viewModel.currentPurity
            observeWallpapers()
        }
    }

    // Initial fetch
    fetchWallpapers()
    Log.d("WallpaperFragment", "Initial fetch with sorting=$sorting")
    viewModel.fetchWallpapers(sorting, "111", "100")
}
    private fun fetchWallpapers() {
        Log.d("WallpaperFragment", "Fetching wallpapers with sorting=$sorting, categories=$categories, purity=$purity")
        viewModel.fetchWallpapers(sorting, categories, purity)
    }

    private fun observeWallpapers() {
        Log.d("WallpaperFragment", "observeWallpapers called for ${if (isRecentTab) "Recent" else "Top"} tab")
        if (isRecentTab) {
            viewModel.recentWallpapers.observe(viewLifecycleOwner) { wallpapers ->
                Log.d("WallpaperFragment", "Received ${wallpapers.size} recent wallpapers")
                updateWallpapers(wallpapers)
            }
        } else {
            viewModel.topWallpapers.observe(viewLifecycleOwner) { wallpapers ->
                Log.d("WallpaperFragment", "Received ${wallpapers.size} top wallpapers")
                updateWallpapers(wallpapers)
            }
        }
    }

    fun updateFilters(categories: String, purity: String) {
        this.categories = categories
        this.purity = purity
        fetchWallpapers()
        refreshWallpapers()
    }

    fun refreshWallpapers() {
        val sorting = if (isRecentTab) "date_added" else "toplist"
        viewModel.fetchWallpapers(sorting, categories, purity)
    }

private fun updateWallpapers(wallpapers: List<Wallpaper>) {
    Log.d("WallpaperFragment", "Updating ${wallpapers.size} wallpapers for ${if (isRecentTab) "Recent" else "Top"} tab")
    try {
        if (wallpapers.isEmpty()) {
            // Show a message or placeholder for empty results
            // For example:
            // emptyStateView.visibility = View.VISIBLE
            // recyclerView.visibility = View.GONE
        } else {
            // emptyStateView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submitList(wallpapers)
            recyclerView.scrollToPosition(0)
            Log.d("WallpaperFragment", "RecyclerView item count after update: ${recyclerView.adapter?.itemCount}")

        }
    } catch (e: Exception) {
        Log.e("WallpaperFragment", "Error in updateWallpapers", e)
    }
}

    companion object {
        private const val ARG_IS_RECENT_TAB = "is_recent_tab"
        private const val ARG_SORTING = "sorting"
        private const val ARG_CATEGORIES = "categories"
        private const val ARG_PURITY = "purity"

        fun newInstance(isRecentTab: Boolean, sorting: String, categories: String, purity: String): WallpaperFragment {
            Log.d("WallpaperFragment", "Creating new instance for ${if (isRecentTab) "Recent" else "Top"} tab with sorting=$sorting, categories=$categories, purity=$purity")
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