package com.example.walls

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.walls.databinding.ActivityFavoritesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private val viewModel: WallpaperViewModel by viewModels()
    private lateinit var adapter: WallpaperAdapter

    private val cropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh the list if the favorite state was changed in CropActivity
            viewModel.fetchFavoriteWallpapers()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = WallpaperAdapter { wallpaper ->
            Intent(this, CropActivity::class.java).apply {
                putExtra("WALLPAPER_ID", wallpaper.id)
                putExtra("IMAGE_URL", wallpaper.url)
            }.also {
                cropActivityResultLauncher.launch(it)
            }
        }

        binding.favoritesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@FavoritesActivity, 2)
            adapter = this@FavoritesActivity.adapter
        }

        // Load favorites when the activity is created
        viewModel.loadFavorites()

        lifecycleScope.launch {
            viewModel.favoriteWallpapers.collectLatest { wallpaperDetails ->
                val wallpapers = wallpaperDetails.map { detail ->
                    Wallpaper(
                        id = detail.id,
                        url = detail.url,
                        path = detail.path,
                        thumbs = Thumbs(
                            large = detail.thumbs.large,
                            original = detail.thumbs.original,
                            small = detail.thumbs.small
                        )
                    )
                }
                updateEmptyState(wallpapers.isEmpty())
                adapter.submitList(wallpapers)
            }
        }
        Log.d("FavoritesActivity", "Fetching favorite wallpapers")
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.favoritesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyStateTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchFavoriteWallpapers()
    }
}
