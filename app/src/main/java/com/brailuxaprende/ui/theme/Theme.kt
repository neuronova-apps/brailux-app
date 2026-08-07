package com.brailuxaprende.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.brailuxaprende.data.settings.TextSizePreference

private val LightColorScheme = lightColorScheme(
    primary = BrailuxBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EBFF),
    onPrimaryContainer = BrailuxBlueDark,
    inversePrimary = BrailuxSky,
    secondary = Color(0xFF1769AA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDEEFF),
    onSecondaryContainer = BrailuxBlueDark,
    tertiary = BrailuxSuccess,
    onTertiary = Color.White,
    tertiaryContainer = BrailuxSuccessContainer,
    onTertiaryContainer = Color(0xFF123915),
    background = BrailuxBackground,
    onBackground = BrailuxText,
    surface = BrailuxSurface,
    onSurface = BrailuxText,
    surfaceVariant = BrailuxSurfaceVariant,
    onSurfaceVariant = BrailuxTextSecondary,
    surfaceTint = BrailuxBlue,
    inverseSurface = BrailuxBlueDark,
    inverseOnSurface = Color.White,
    error = BrailuxError,
    onError = Color.White,
    errorContainer = BrailuxErrorContainer,
    onErrorContainer = Color(0xFF410E0B),
    outline = BrailuxOutline,
    outlineVariant = Color(0xFFC0CDDA),
    scrim = Color.Black,
)

private val HighContrastColorScheme = darkColorScheme(
    primary = HighContrastPrimary,
    onPrimary = HighContrastBackground,
    primaryContainer = HighContrastForeground,
    onPrimaryContainer = HighContrastBackground,
    inversePrimary = HighContrastPrimary,
    secondary = HighContrastSecondary,
    onSecondary = HighContrastBackground,
    secondaryContainer = HighContrastForeground,
    onSecondaryContainer = HighContrastBackground,
    tertiary = HighContrastSuccess,
    onTertiary = HighContrastBackground,
    tertiaryContainer = HighContrastBackground,
    onTertiaryContainer = HighContrastSuccess,
    background = HighContrastBackground,
    onBackground = HighContrastForeground,
    surface = HighContrastBackground,
    onSurface = HighContrastForeground,
    surfaceVariant = HighContrastSurfaceVariant,
    onSurfaceVariant = HighContrastForeground,
    surfaceTint = HighContrastPrimary,
    inverseSurface = HighContrastForeground,
    inverseOnSurface = HighContrastBackground,
    error = HighContrastError,
    onError = HighContrastBackground,
    errorContainer = HighContrastBackground,
    onErrorContainer = HighContrastError,
    outline = HighContrastForeground,
    outlineVariant = HighContrastOutlineVariant,
    scrim = HighContrastBackground,
)

object BrailuxTheme {
    val statusColors: BrailuxStatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBrailuxStatusColors.current
}

@Composable
fun BrailuxAprendeTheme(
    highContrast: Boolean = false,
    textSize: TextSizePreference = TextSizePreference.Normal,
    content: @Composable () -> Unit,
) {
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * textSize.scaleFactor,
    )

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalBrailuxStatusColors provides if (highContrast) {
            HighContrastStatusColors
        } else {
            RegularStatusColors
        },
    ) {
        MaterialTheme(
            colorScheme = if (highContrast) HighContrastColorScheme else LightColorScheme,
            typography = BrailuxTypography,
            shapes = BrailuxShapes,
            content = content,
        )
    }
}
