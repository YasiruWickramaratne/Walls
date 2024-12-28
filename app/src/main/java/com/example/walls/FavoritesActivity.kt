package com.example.walls

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
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        viewModel = ViewModelProvider(this)[WallpaperViewModel::class.java]

        adapter = WallpaperAdapter { wallpaper ->
            // Handle item click
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wallpaper.url))
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.favoritesRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        emptyStateTextView = findViewById(R.id.emptyStateTextView)

        viewModel.favoriteWallpapers.observe(this) { wallpapers ->
            if (wallpapers.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyStateTextView.visibility = View.VISIBLE
                emptyStateTextView.text = "No favorite wallpapers yet"
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyStateTextView.visibility = View.GONE
                // Map FavoritesManager.WallpaperDetail to Wallpaper
                val mappedWallpapers = wallpapers.map { detail ->
                    Wallpaper(
                        id = detail.id,
                        url = detail.url,
                        path = detail.path,  // Add this line for the full resolution image URL
                        thumbs = Thumbs(
                            small = detail.thumbs.small,
                            original = detail.thumbs.original,
                            large = detail.thumbs.large
                        )

                    )
                }
                adapter.submitList(mappedWallpapers)
            }
        }

        viewModel.fetchFavoriteWallpapers()
        Log.d("FavoritesActivity", "Fetching favorite wallpapers")
    }


    override fun onResume() {
        super.onResume()
        viewModel.fetchFavoriteWallpapers()
        Log.d("FavoritesActivity", "Refreshing favorite wallpapers in onResume")
    }
}