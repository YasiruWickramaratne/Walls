package com.example.walls


import FavoritesManager
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.example.walls.databinding.ActivityFullScreenImageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullScreenImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullScreenImageBinding
    private lateinit var viewModel: WallpaperViewModel
    private var currentWallpaperId: String? = null
    private var currentWallpaperUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        val favoritesManager = FavoritesManager(application)
        val factory = WallpaperViewModelFactory(application, favoritesManager)
        viewModel = ViewModelProvider(this, factory)[WallpaperViewModel::class.java]

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
            val isFavorite = viewModel.isFavorite(id)
            updateFavoriteButtonState(isFavorite)
            binding.favoriteButton.setOnClickListener {
                viewModel.toggleFavorite(id)
                updateFavoriteButtonState(!isFavorite)
            }
        }
    }

    private fun updateFavoriteButtonState(isFavorite: Boolean) {
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