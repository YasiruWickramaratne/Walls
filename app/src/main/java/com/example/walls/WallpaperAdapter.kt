import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.example.walls.R
import com.example.walls.Wallpaper


class WallpaperAdapter : ListAdapter<Wallpaper, WallpaperAdapter.ViewHolder>(WallpaperDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wallpaper = getItem(position)
        Log.d("WallpaperAdapter", "Binding wallpaper at position $position: ${wallpaper.id}")
        holder.bind(wallpaper)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.wallpaper_image_view)
        private val titleTextView: TextView = itemView.findViewById(R.id.wallpaper_title_text_view)

        fun bind(wallpaper: Wallpaper) {
            Log.d("WallpaperAdapter", "Binding wallpaper: id=${wallpaper.id}, url=${wallpaper.thumbs.small}")
            
            Glide.with(itemView.context)
                .load(wallpaper.thumbs.small)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        Log.e("WallpaperAdapter", "Failed to load image: ${wallpaper.thumbs.small}", e)
                        return false
                    }
        
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        Log.d("WallpaperAdapter", "Image loaded successfully: ${wallpaper.thumbs.small}")
                        return false
                    }
                })
                .into(imageView)
        
            titleTextView.text = wallpaper.id
        
            itemView.setOnClickListener {
                Log.d("WallpaperAdapter", "Wallpaper clicked: ${wallpaper.id}")
                // Handle click event, e.g., open full-size wallpaper
            }
        }
    }

    class WallpaperDiffCallback : DiffUtil.ItemCallback<Wallpaper>() {
        override fun areItemsTheSame(oldItem: Wallpaper, newItem: Wallpaper): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Wallpaper, newItem: Wallpaper): Boolean {
            return oldItem == newItem
        }
    }
}