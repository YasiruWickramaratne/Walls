package com.example.walls.ui


import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.example.walls.databinding.ActivityFullScreenImageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.viewModels
import com.example.walls.R
import com.example.walls.WallpaperViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class FullScreenImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullScreenImageBinding
    private val viewModel: WallpaperViewModel by viewModels()
    private var currentWallpaperId: String? = null
    private var currentWallpaperUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentWallpaperId = intent.getStringExtra("WALLPAPER_ID")
        currentWallpaperUrl = intent.getStringExtra("IMAGE_URL")

        if (currentWallpaperId == null || currentWallpaperUrl == null) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadImage()
        setupFavoriteButton()
        observeFavorites()
    }

    private fun setupViews() {
        binding.setHomeScreenButton.setOnClickListener {
            setWallpaper(currentWallpaperUrl!!, WallpaperManager.FLAG_SYSTEM)
        }

        binding.setLockScreenButton.setOnClickListener {
            setWallpaper(currentWallpaperUrl!!, WallpaperManager.FLAG_LOCK)
        }

        binding.setBothScreensButton.setOnClickListener {
            setWallpaper(currentWallpaperUrl!!, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        }
    }

    private fun loadImage() {
        Glide.with(this)
            .asBitmap()
            .load(currentWallpaperUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    binding.fullScreenImageView.setImage(ImageSource.bitmap(resource))
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    Toast.makeText(this@FullScreenImageActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupFavoriteButton() {
        currentWallpaperId?.let { id ->
            updateFavoriteButtonState(viewModel.isFavorite(id))
            binding.favoriteButton.setOnClickListener {
                viewModel.toggleFavorite(id)
            }
        }
    }

    private fun observeFavorites() {
        lifecycleScope.launch {
            viewModel.favorites.collectLatest { favorites ->
                currentWallpaperId?.let { id ->
                    Log.d("FullScreenImageActivity", "Favorites updated: $favorites, current id: $id")
                    updateFavoriteButtonState(favorites.contains(id))
                }
            }
        }
    }

    private fun updateFavoriteButtonState(isFavorite: Boolean) {
        Log.d("FullScreenImageActivity", "Updating favorite button state: $isFavorite")
        binding.favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        )
    }

    private fun setWallpaper(imageUrl: String, flag: Int) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(this@FullScreenImageActivity)
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get()
                }

                val wallpaperManager = WallpaperManager.getInstance(this@FullScreenImageActivity)

                withContext(Dispatchers.IO) {
                    wallpaperManager.setBitmap(bitmap, null, true, flag)
                }

                Toast.makeText(
                    this@FullScreenImageActivity,
                    "Wallpaper set successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@FullScreenImageActivity,
                    "Failed to set wallpaper",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }
}