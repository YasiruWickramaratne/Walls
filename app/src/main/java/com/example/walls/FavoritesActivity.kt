package com.example.walls

import FavoritesManager
import WallpaperAdapter
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.TextView

class FavoritesActivity : AppCompatActivity() {

    private lateinit var viewModel: WallpaperViewModel
    private lateinit var adapter: WallpaperAdapter
    private lateinit var emptyStateTextView: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        val favoritesManager = FavoritesManager(application)
        val factory = WallpaperViewModelFactory(application, favoritesManager)
        viewModel = ViewModelProvider(this, factory)[WallpaperViewModel::class.java]

        adapter = WallpaperAdapter { wallpaper ->
            // Handle item click
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wallpaper.url))
            startActivity(intent)
        }

        // Set up RecyclerView
        findViewById<RecyclerView>(R.id.favoritesRecyclerView).apply {
            layoutManager = GridLayoutManager(this@FavoritesActivity, 2)
            adapter = this@FavoritesActivity.adapter
        }

        emptyStateTextView = findViewById(R.id.emptyStateTextView)

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
        findViewById<RecyclerView>(R.id.favoritesRecyclerView).visibility =
            if (isEmpty) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.emptyStateTextView).visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchFavoriteWallpapers()
    }
}