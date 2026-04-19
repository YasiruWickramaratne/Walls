package com.example.walls.ui.favorites

sealed interface FavoritesUiMode {
    data object BrowsingDefault : FavoritesUiMode
    data class BrowsingCollection(val name: String) : FavoritesUiMode
    data class Selecting(
        val ids: Set<String>,
        val collectionName: String?
    ) : FavoritesUiMode
}
