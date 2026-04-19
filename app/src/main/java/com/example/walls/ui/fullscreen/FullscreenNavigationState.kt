package com.example.walls.ui.fullscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class FullscreenNavigationState(initialIndex: Int) {
    var currentIndex by mutableIntStateOf(initialIndex)
        private set

    fun syncToBounds(lastIndex: Int) {
        if (lastIndex < 0) return
        currentIndex = currentIndex.coerceIn(0, lastIndex)
    }

    fun canGoPrevious(): Boolean = currentIndex > 0

    fun showPrevious() {
        if (currentIndex > 0) {
            currentIndex--
        }
    }

    fun showNext(lastIndex: Int): Boolean {
        if (currentIndex < lastIndex) {
            currentIndex++
            return true
        }
        return false
    }
}

@Composable
fun rememberFullscreenNavigationState(initialIndex: Int): FullscreenNavigationState {
    return remember(initialIndex) { FullscreenNavigationState(initialIndex) }
}
