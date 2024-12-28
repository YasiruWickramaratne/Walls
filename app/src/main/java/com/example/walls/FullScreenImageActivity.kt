package com.example.walls


import FavoritesManager
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullScreenImageActivity : AppCompatActivity() {
    private lateinit var viewModel: WallpaperViewModel
    private lateinit var favoriteButton: ImageButton
    private var currentWallpaperId: String? = null
    private var currentWallpaperUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FullScreenImageActivity", "onCreate started")
        setContentView(R.layout.activity_full_screen_image)

        setupFavoriteButton()

        try {
            // Initialize ViewModel
            Log.d("FullScreenImageActivity", "Initializing ViewModel")
            val favoritesManager = FavoritesManager(application)
            val factory = WallpaperViewModelFactory(application, favoritesManager)
            viewModel = ViewModelProvider(this, factory)[WallpaperViewModel::class.java]

            Log.d("FullScreenImageActivity", "ViewModel initialized")

            // Initialize ViewModel

            Log.d("FullScreenImageActivity", "Getting ViewModel instance")
            viewModel = ViewModelProvider(this, factory)[WallpaperViewModel::class.java]
            Log.d("FullScreenImageActivity", "ViewModel initialized")

            currentWallpaperId = intent.getStringExtra("WALLPAPER_ID")
            currentWallpaperUrl = intent.getStringExtra("IMAGE_URL")
            Log.d(
                "FullScreenImageActivity",
                "Wallpaper ID: $currentWallpaperId, URL: $currentWallpaperUrl"
            )

            if (currentWallpaperId == null || currentWallpaperUrl == null) {
                Log.e("FullScreenImageActivity", "Wallpaper ID or URL is null")
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            Log.d("FullScreenImageActivity", "Finding views")
            val imageView: SubsamplingScaleImageView = findViewById(R.id.full_screen_image_view)
            val setHomeScreenButton: Button = findViewById(R.id.set_home_screen_button)
            val setLockScreenButton: Button = findViewById(R.id.set_lock_screen_button)
            val setBothScreensButton: Button = findViewById(R.id.set_both_screens_button)
            favoriteButton = findViewById(R.id.favoriteButton)
            Log.d("FullScreenImageActivity", "Views found")

            // Load image using Glide and set it to SubsamplingScaleImageView
            Log.d("FullScreenImageActivity", "Loading image with Glide")
            Glide.with(this)
                .asBitmap()
                .load(currentWallpaperUrl)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        Log.d("FullScreenImageActivity", "Image loaded successfully")
                        imageView.setImage(ImageSource.bitmap(resource))
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        Log.d("FullScreenImageActivity", "Image load cleared")
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        Log.e("FullScreenImageActivity", "Failed to load image")
                        super.onLoadFailed(errorDrawable)
                    }
                })

            // Set up button click listeners
            setHomeScreenButton.setOnClickListener {
                Log.d("FullScreenImageActivity", "Home screen button clicked")
                setWallpaper(currentWallpaperUrl!!, WallpaperManager.FLAG_SYSTEM)
            }

            setLockScreenButton.setOnClickListener {
                Log.d("FullScreenImageActivity", "Lock screen button clicked")
                setWallpaper(currentWallpaperUrl!!, WallpaperManager.FLAG_LOCK)
            }

            setBothScreensButton.setOnClickListener {
                Log.d("FullScreenImageActivity", "Both screens button clicked")
                setWallpaper(
                    currentWallpaperUrl!!,
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                )
            }

            // Set up favorite button
            Log.d("FullScreenImageActivity", "Setting up favorite button")
            setupFavoriteButton()

        } catch (e: Exception) {
            Log.e("FullScreenImageActivity", "Error in onCreate", e)
            Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
        Log.d("FullScreenImageActivity", "onCreate completed")
    }

    private fun setupFavoriteButton() {
        currentWallpaperId?.let { id ->
            val isFavorite = viewModel.isFavorite(id)
            updateFavoriteButtonState(isFavorite)
            favoriteButton.setOnClickListener {
                viewModel.toggleFavorite(id)
                updateFavoriteButtonState(!isFavorite)
            }
        }
    }

    private fun updateFavoriteButtonState(isFavorite: Boolean) {
        Log.d("FullScreenImageActivity", "Updating favorite button state, isFavorite: $isFavorite")
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border)
        }
        Log.d("FullScreenImageActivity", "Favorite button state updated, isFavorite: $isFavorite")
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