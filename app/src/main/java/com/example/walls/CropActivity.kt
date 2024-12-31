package com.example.walls

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

        setupFavoriteButton()
        setupBackButton()

        // Set the OnCropImageCompleteListener here
        binding.cropImageView.setOnCropImageCompleteListener { _, result ->
            val croppedUri = result.uriContent
            Log.d("CropActivity", "Cropped image URI: $croppedUri")
            // Handle the cropped image URI, e.g., save it or set it as wallpaper
            //setWallpaper(croppedUri)
        }

        loadWallpaperDetails()
        setupViews() // Keep this after setting the listener
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
} 