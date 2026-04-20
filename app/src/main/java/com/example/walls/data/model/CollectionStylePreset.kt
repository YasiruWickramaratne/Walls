package com.example.walls.data.model

enum class CollectionStylePreset(
    val label: String,
    val description: String
) {
    DEFAULT("Balanced", "Balanced crop for mixed wallpapers"),
    AMOLED_DARK("AMOLED Dark", "Prefer dark, contrasty areas"),
    PORTRAIT_FOCUS("Portrait Focus", "Keep faces and subjects centered"),
    NATURE_SCENIC("Nature Scenic", "Prefer open scenic framing"),
    NEON_POP("Neon Pop", "Favor vibrant colorful sections"),
    MINIMAL_CLEAN("Minimal Clean", "Bias toward calm uncluttered framing")
}
