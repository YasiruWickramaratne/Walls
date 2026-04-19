package com.example.walls.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.walls.R
import com.example.walls.Wallpaper

class WallpaperAdapter(private val onItemClick: (Wallpaper) -> Unit) :
    ListAdapter<Wallpaper, WallpaperAdapter.ViewHolder>(WallpaperDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wallpaper = getItem(position)
        holder.bind(wallpaper)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.wallpaper_image_view)
        private val titleTextView: TextView = itemView.findViewById(R.id.wallpaper_title_text_view)

        fun bind(wallpaper: Wallpaper) {
            imageView.load(wallpaper.thumbs.small) {
                crossfade(true)
                placeholder(R.drawable.placeholder_image)
                error(R.drawable.error_image)
            }

            titleTextView.text = wallpaper.id

            itemView.setOnClickListener {
                onItemClick(wallpaper)
            }
        }
    }

    private class WallpaperDiffCallback : DiffUtil.ItemCallback<Wallpaper>() {
        override fun areItemsTheSame(oldItem: Wallpaper, newItem: Wallpaper): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Wallpaper, newItem: Wallpaper): Boolean {
            return oldItem == newItem
        }
    }
}