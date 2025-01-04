package com.example.walls.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walls.R
import com.example.walls.data.repository.WallpaperRepository
import com.example.walls.WallpaperViewModel
import com.example.walls.api.WallhavenApiService
import com.example.walls.databinding.ActivityCropBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropBinding
    private val viewModel: WallpaperViewModel by viewModels()
    private var currentWallpaperId: String? = null
    private var currentWallpaperUrl: String? = null
    private var isFavorite: Boolean = false
    private var favoriteStateChanged: Boolean = false // To track if the favorite state was changed

    @Inject
    lateinit var apiService: WallhavenApiService

    @Inject
    lateinit var wallpaperRepository: WallpaperRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentWallpaperId = intent.getStringExtra("WALLPAPER_ID")
        currentWallpaperUrl = intent.getStringExtra("IMAGE_URL")

        Log.d("CropActivity", "onCreate: WALLPAPER_ID = $currentWallpaperId")

        setupFavoriteButton()
        setupBackButton()

        // Set the OnCropImageCompleteListener here
        binding.cropImageView.setOnCropImageCompleteListener { _, result ->
            val croppedUri = result.uriContent
            Log.d("CropActivity", "Cropped image URI: $croppedUri")
            Toast.makeText(this@CropActivity, "Cropped image saved!", Toast.LENGTH_SHORT).show()
            saveCropRect() // Save the crop rect after a successful crop
        }

        loadWallpaperDetails()
        setupViews()
    }

    private fun setupBackButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadWallpaperDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiKey = wallpaperRepository.getApiKey()
                val response = apiService.getWallpaperDetails(currentWallpaperId!!, apiKey)
                withContext(Dispatchers.Main) {
                    currentWallpaperUrl = response.data.path
                    loadImageToCropView()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CropActivity, "Error fetching image details", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun loadImageToCropView() {
        currentWallpaperUrl?.let { urlString ->
            lifecycleScope.launch(Dispatchers.IO) {
                var connection: HttpURLConnection? = null
                var inputStream: InputStream? = null
                try {
                    val url = URL(urlString)
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
                    )
                    connection.connectTimeout = 15000 // 15 seconds
                    connection.readTimeout = 15000 // 15 seconds

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        inputStream = connection.inputStream
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        withContext(Dispatchers.Main) {
                            if (bitmap != null) {
                                binding.cropImageView.setImageBitmap(bitmap)
                                loadSavedCropRect() // Call loadSavedCropRect() after setting the bitmap
                            } else {
                                Log.e("CropActivity", "loadImageToCropView: Decoded Bitmap is null")
                                Toast.makeText(this@CropActivity, "Error loading image: Could not decode", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("CropActivity", "loadImageToCropView: HTTP error code: $responseCode, error: $errorStream")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CropActivity, "Error loading image: HTTP $responseCode", Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (e: Exception) {
                    Log.e("CropActivity", "loadImageToCropView: Error loading image", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CropActivity, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    inputStream?.close()
                    connection?.disconnect()
                }
            }
        }
    }

    private fun setupViews() {
        binding.cropButton.setOnClickListener {
            binding.cropImageView.croppedImageAsync()
        }
    }

    private fun setupFavoriteButton() {
        currentWallpaperId?.let { id ->
            isFavorite = viewModel.isFavorite(id)
            updateFavoriteButtonState()

            binding.favoriteButton.setOnClickListener {
                viewModel.toggleFavorite(id)
                isFavorite = !isFavorite
                favoriteStateChanged = true // Mark that the favorite state has changed
                updateFavoriteButtonState()
            }
        }
    }

    private fun updateFavoriteButtonState() {
        binding.favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        )
    }

    private fun saveCropRect() {
        currentWallpaperId?.let { id ->
            val cropRect = binding.cropImageView.cropRect
            val imageWidth = binding.cropImageView.wholeImageRect?.width() ?: 0
            val imageHeight = binding.cropImageView.wholeImageRect?.height() ?: 0
            
            Log.d("CropActivity", "Saving crop rect for ID: $id, Rect = $cropRect")
            Log.d("CropActivity", "Original image dimensions: ${imageWidth}x${imageHeight}")
            
            if (cropRect != null) {
                // Save both crop rect and relative percentages
                getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE).edit().apply {
                    // Save absolute coordinates
                    putInt("crop_rect_left_$id", cropRect.left)
                    putInt("crop_rect_top_$id", cropRect.top)
                    putInt("crop_rect_right_$id", cropRect.right)
                    putInt("crop_rect_bottom_$id", cropRect.bottom)
                    
                    // Save relative percentages
                    putFloat("crop_rect_left_percent_$id", cropRect.left.toFloat() / imageWidth)
                    putFloat("crop_rect_top_percent_$id", cropRect.top.toFloat() / imageHeight)
                    putFloat("crop_rect_right_percent_$id", cropRect.right.toFloat() / imageWidth)
                    putFloat("crop_rect_bottom_percent_$id", cropRect.bottom.toFloat() / imageHeight)
                    
                    // Save original dimensions
                    putInt("original_width_$id", imageWidth)
                    putInt("original_height_$id", imageHeight)
                    
                    apply()
                }
            }
        }
    }

    private fun loadSavedCropRect() {
        currentWallpaperId?.let { id ->
            Log.d("CropActivity", "Loading saved crop rect for ID: $id")
            val prefs = getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)
            val savedLeft = prefs.getInt("crop_rect_left_$id", 0)
            val savedTop = prefs.getInt("crop_rect_top_$id", 0)
            val savedRight = prefs.getInt("crop_rect_right_$id", 0)
            val savedBottom = prefs.getInt("crop_rect_bottom_$id", 0)

            Log.d("CropActivity", "Loaded crop rect values: left=$savedLeft, top=$savedTop, right=$savedRight, bottom=$savedBottom")

            if (savedLeft != 0 || savedTop != 0 || savedRight != 0 || savedBottom != 0) {
                val savedRect = Rect(savedLeft, savedTop, savedRight, savedBottom)
                Log.d("CropActivity", "Setting crop rect from saved values for ID: $id, Rect = $savedRect")
                binding.cropImageView.cropRect = savedRect

                Log.d("CropActivity", "Crop rect set on ImageView: ${binding.cropImageView.cropRect}")
            } else {
                Log.d("CropActivity", "No saved crop rect found or invalid values.")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
} 