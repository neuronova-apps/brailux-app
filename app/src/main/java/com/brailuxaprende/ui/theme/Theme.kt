package com.brailuxaprende.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.brailuxaprende.data.settings.TextSizePreference

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
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
    tertiary = HighContrastPrimary,
    onTertiary = HighContrastBackground,
    tertiaryContainer = HighContrastForeground,
    onTertiaryContainer = HighContrastBackground,
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
    errorContainer = HighContrastForeground,
    onErrorContainer = HighContrastBackground,
    outline = HighContrastForeground,
    outlineVariant = HighContrastOutlineVariant,
    scrim = HighContrastBackground,
)

@Composable
fun BrailuxAprendeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    textSize: TextSizePreference = TextSizePreference.Normal,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast -> HighContrastColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * textSize.scaleFactor,
    )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
