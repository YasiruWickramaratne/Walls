package com.example.walls

import FavoritesManager
import WallpaperAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.walls.databinding.ActivityFavoritesBinding

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var viewModel: WallpaperViewModel
    private lateinit var adapter: WallpaperAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val favoritesManager = FavoritesManager(application)
        val factory = WallpaperViewModelFactory(application, favoritesManager)
        viewModel = ViewModelProvider(this, factory)[WallpaperViewModel::class.java]

        adapter = WallpaperAdapter { wallpaper ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wallpaper.url))
            startActivity(intent)
        }

        binding.favoritesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@FavoritesActivity, 2)
            adapter = this@FavoritesActivity.adapter
        }

        viewModel.favoriteWallpapers.observe(this) { wallpaperDetails ->
            val wallpapers = wallpaperDetails.map { detail ->
                Wallpaper(
                    id = detail.id,
                    url = detail.url,
                    path = detail.path,
                    thumbs = Thumbs(
                        small = detail.thumbs.small,
                        original = detail.thumbs.original,
                        large = detail.thumbs.large
                    )
                )
            }
            updateEmptyState(wallpapers.isEmpty())
            adapter.submitList(wallpapers)
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