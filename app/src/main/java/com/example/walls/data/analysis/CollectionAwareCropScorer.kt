package com.example.walls.data.analysis

import com.example.walls.data.model.CollectionStylePreset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionAwareCropScorer @Inject constructor() {

    fun applyStyle(baseScore: Float, paletteScore: Float, stylePreset: CollectionStylePreset): Float {
        val styled = when (stylePreset) {
            CollectionStylePreset.AMOLED_DARK -> baseScore + (1f - paletteScore) * 0.12f
            CollectionStylePreset.PORTRAIT_FOCUS -> baseScore + 0.08f
            CollectionStylePreset.NATURE_SCENIC -> baseScore + 0.05f
            CollectionStylePreset.NEON_POP -> baseScore + paletteScore * 0.12f
            CollectionStylePreset.MINIMAL_CLEAN -> baseScore + (1f - paletteScore) * 0.06f
            CollectionStylePreset.DEFAULT -> baseScore
        }
        return styled.coerceIn(0f, 1f)
    }
}
