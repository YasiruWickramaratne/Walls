package com.example.walls

import android.app.WallpaperManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: return

        val imageView: ImageView = findViewById(R.id.full_screen_image_view)
        val setHomeScreenButton: Button = findViewById(R.id.set_home_screen_button)
        val setLockScreenButton: Button = findViewById(R.id.set_lock_screen_button)
        val setBothScreensButton: Button = findViewById(R.id.set_both_screens_button)

        Glide.with(this)
            .load(imageUrl)
            .into(imageView)

        setHomeScreenButton.setOnClickListener {
            setWallpaper(imageUrl, WallpaperManager.FLAG_SYSTEM)
        }

        setLockScreenButton.setOnClickListener {
            setWallpaper(imageUrl, WallpaperManager.FLAG_LOCK)
        }

        setBothScreensButton.setOnClickListener {
            setWallpaper(imageUrl, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        }
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

                Toast.makeText(this@FullScreenImageActivity, "Wallpaper set successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@FullScreenImageActivity, "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}