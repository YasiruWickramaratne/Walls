import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.walls.WallpaperViewModel

class WallpaperViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WallpaperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WallpaperViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}