package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// StitchByHer Brand Palette
val CreamBackground = Color(0xFFFFF8F3)
val WarmCreamSurface = Color(0xFFFDF2EC)
val SoftBlushSurface = Color(0xFFFAEBED)
val BlushPink = Color(0xFFE8B7BE)
val SoftRose = Color(0xFFD9A0AA)
val DeepRose = Color(0xFFB56E7C)
val ObsidianBlack = Color(0xFF171313)
val CharcoalInk = Color(0xFF332C2C)
val MutedTaupe = Color(0xFF7D7172)
val HairlineRose = Color(0xFFEBD4D7)
val PureWhite = Color(0xFFFFFFFF)
val ChampagneGold = Color(0xFFC5A06B)
val EmeraldSage = Color(0xFF2F7A5B)
val WhatsAppGreen = Color(0xFF1B8C4F)
val CrimsonError = Color(0xFFB83B4B)

fun parseHexColorOrFallback(hex: String, fallback: Color): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        when (cleaned.length) {
            6 -> Color(("FF$cleaned").toLong(16))
            8 -> Color(cleaned.toLong(16))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
