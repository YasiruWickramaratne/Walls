package com.example.walls.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.walls.data.local.analysis.WallpaperAnalysisDao
import com.example.walls.data.local.analysis.WallpaperAnalysisEntity

@Database(
    entities = [WallpaperAnalysisEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WallsDatabase : RoomDatabase() {
    abstract fun wallpaperAnalysisDao(): WallpaperAnalysisDao
}
