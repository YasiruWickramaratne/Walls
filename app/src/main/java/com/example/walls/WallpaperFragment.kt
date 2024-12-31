package com.example.walls

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WallpaperFragment : Fragment(), FilterUpdateListener {
    private val viewModel: WallpaperViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WallpaperAdapter
    private lateinit var layoutManager: GridLayoutManager
    
    // Add variables to store scroll position
    private var lastFirstVisiblePosition = 0

    private val isRecentTab: Boolean by lazy { arguments?.getBoolean(ARG_IS_RECENT_TAB) ?: true }
    private val sorting: String by lazy { arguments?.getString(ARG_SORTING) ?: "date_added" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallpaper, container, false)
    }

    override fun onPause() {
        super.onPause()
        // Save scroll position when leaving the fragment
        lastFirstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
    }

    override fun onResume() {
        super.onResume()
        // Restore scroll position when returning to the fragment
        if (lastFirstVisiblePosition > 0) {
            recyclerView.scrollToPosition(lastFirstVisiblePosition)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize recyclerView first
        recyclerView = view.findViewById(R.id.recyclerView)
        
        // Then initialize layoutManager and setup recyclerView
        layoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = layoutManager
        
        adapter = WallpaperAdapter { wallpaper ->
            openFullScreenImage(wallpaper)
        }
        recyclerView.adapter = adapter

        // Collect wallpapers without clearing the list
        viewLifecycleOwner.lifecycleScope.launch {
            if (isRecentTab) {
                viewModel.recentWallpapers.collectLatest { wallpapers ->
                    adapter.submitList(wallpapers) {
                        // Restore position after list update if needed
                        if (lastFirstVisiblePosition > 0) {
                            recyclerView.scrollToPosition(lastFirstVisiblePosition)
                        }
                    }
                }
            } else {
                viewModel.topWallpapers.collectLatest { wallpapers ->
                    adapter.submitList(wallpapers) {
                        // Restore position after list update if needed
                        if (lastFirstVisiblePosition > 0) {
                            recyclerView.scrollToPosition(lastFirstVisiblePosition)
                        }
                    }
                }
            }
        }

        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Update last known position while scrolling
                lastFirstVisiblePosition = firstVisibleItemPosition

                // Check if we need to load more items
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                    && firstVisibleItemPosition >= 0) {
                    loadMoreWallpapers()
                }
            }
        })
    }

    override fun onFilterUpdated(categories: String, purity: String) {
        // Reset scroll position when filters change
        lastFirstVisiblePosition = 0
        viewModel.resetPagination()
        viewModel.fetchWallpapers(sorting)
    }

    private fun openFullScreenImage(wallpaper: Wallpaper) {
        val intent = Intent(requireContext(), FullScreenImageActivity::class.java).apply {
            putExtra("WALLPAPER_ID", wallpaper.id)
            putExtra("IMAGE_URL", wallpaper.path)
        }
        startActivity(intent)
    }

    private fun loadMoreWallpapers() {
        viewModel.fetchWallpapers(sorting, isLoadingMore = true)
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