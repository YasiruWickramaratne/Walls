package com.example.walls

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: return

        val imageView: SubsamplingScaleImageView = findViewById(R.id.full_screen_image_view)
        val setHomeScreenButton: Button = findViewById(R.id.set_home_screen_button)
        val setLockScreenButton: Button = findViewById(R.id.set_lock_screen_button)
        val setBothScreensButton: Button = findViewById(R.id.set_both_screens_button)

        // Load image using Glide and set it to SubsamplingScaleImageView
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    imageView.setImage(ImageSource.bitmap(resource))
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // This is called when imageView is cleared on lifecycle call or for some other reason.
                    // If you are referencing the bitmap somewhere else too other than this imageView,
                    // clear it here as you can no longer have the bitmap.
                }
            })

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