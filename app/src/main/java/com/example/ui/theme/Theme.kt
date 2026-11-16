package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val BoutiqueShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun StitchByHerTheme(
    primaryHex: String = "#E8B7BE",
    accentHex: String = "#D9A0AA",
    content: @Composable () -> Unit
) {
    val dynamicBlush = parseHexColorOrFallback(primaryHex, BlushPink)
    val dynamicRose = parseHexColorOrFallback(accentHex, SoftRose)

    val colorScheme = lightColorScheme(
        primary = ObsidianBlack,
        onPrimary = PureWhite,
        primaryContainer = dynamicBlush,
        onPrimaryContainer = ObsidianBlack,
        secondary = dynamicRose,
        onSecondary = ObsidianBlack,
        secondaryContainer = SoftBlushSurface,
        onSecondaryContainer = CharcoalInk,
        tertiary = ChampagneGold,
        onTertiary = PureWhite,
        background = CreamBackground,
        onBackground = ObsidianBlack,
        surface = CreamBackground,
        onSurface = ObsidianBlack,
        surfaceVariant = WarmCreamSurface,
        onSurfaceVariant = MutedTaupe,
        surfaceContainerLowest = PureWhite,
        surfaceContainerLow = WarmCreamSurface,
        surfaceContainer = SoftBlushSurface,
        surfaceContainerHigh = dynamicBlush.copy(alpha = 0.25f),
        outline = HairlineRose,
        outlineVariant = dynamicRose.copy(alpha = 0.4f),
        error = CrimsonError,
        onError = PureWhite
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BoutiqueShapes,
        content = content
    )
}
