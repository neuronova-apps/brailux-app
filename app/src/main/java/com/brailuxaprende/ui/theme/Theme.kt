package com.brailuxaprende.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.brailuxaprende.data.seasonal.SeasonalAccent
import com.brailuxaprende.data.settings.AppearancePreference
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

private val DarkColorScheme = darkColorScheme(
    primary = BrailuxDarkPrimary,
    onPrimary = Color(0xFF003353),
    primaryContainer = BrailuxDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFD6ECFF),
    inversePrimary = BrailuxBlue,
    secondary = Color(0xFF74C7F5),
    onSecondary = Color(0xFF00354D),
    secondaryContainer = Color(0xFF16455E),
    onSecondaryContainer = Color(0xFFC7E9FF),
    tertiary = Color(0xFF80D982),
    onTertiary = Color(0xFF00390A),
    tertiaryContainer = Color(0xFF0C4A1E),
    onTertiaryContainer = Color(0xFFA1F3A3),
    background = BrailuxDarkBackground,
    onBackground = BrailuxDarkText,
    surface = BrailuxDarkSurface,
    onSurface = BrailuxDarkText,
    surfaceVariant = BrailuxDarkSurfaceVariant,
    onSurfaceVariant = BrailuxDarkTextSecondary,
    surfaceTint = BrailuxDarkPrimary,
    inverseSurface = BrailuxDarkText,
    inverseOnSurface = BrailuxDarkBackground,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = BrailuxDarkOutline,
    outlineVariant = BrailuxDarkOutlineVariant,
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

    val visual: BrailuxThemeVisualDefinition
        @Composable
        @ReadOnlyComposable
        get() = LocalBrailuxTheme.current.visual

    val current: BrailuxThemeDefinition
        @Composable
        @ReadOnlyComposable
        get() = LocalBrailuxTheme.current
}

internal enum class BrailuxThemeVariant {
    Light,
    Dark,
    HighContrast,
}

internal fun resolveThemeVariant(
    appearance: AppearancePreference,
    systemInDarkTheme: Boolean,
    highContrast: Boolean,
): BrailuxThemeVariant = when {
    highContrast -> BrailuxThemeVariant.HighContrast
    appearance == AppearancePreference.Light -> BrailuxThemeVariant.Light
    appearance == AppearancePreference.Dark -> BrailuxThemeVariant.Dark
    systemInDarkTheme -> BrailuxThemeVariant.Dark
    else -> BrailuxThemeVariant.Light
}

@Composable
fun BrailuxAprendeTheme(
    appearance: AppearancePreference = AppearancePreference.Light,
    highContrast: Boolean = false,
    textSize: TextSizePreference = TextSizePreference.Normal,
    seasonalAccent: SeasonalAccent? = null,
    customBackgroundVisible: Boolean = false,
    themeDefinition: BrailuxThemeDefinition = BrailuxThemeCatalog.defaultTheme,
    content: @Composable () -> Unit,
) {
    val themeVariant = resolveThemeVariant(
        appearance = appearance,
        systemInDarkTheme = isSystemInDarkTheme(),
        highContrast = highContrast,
    )
    val baseColorScheme = when (themeVariant) {
        BrailuxThemeVariant.Light -> LightColorScheme
        BrailuxThemeVariant.Dark -> DarkColorScheme
        BrailuxThemeVariant.HighContrast -> HighContrastColorScheme
    }
    val effectiveThemeDefinition = if (highContrast) {
        BrailuxThemeCatalog.highContrastTheme
    } else {
        themeDefinition
    }
    val themedColorScheme = if (themeVariant == BrailuxThemeVariant.HighContrast) {
        baseColorScheme
    } else {
        val adaptedScheme = if (effectiveThemeDefinition.premium) {
            baseColorScheme.copy(
                primary = effectiveThemeDefinition.visual.primary,
                secondary = effectiveThemeDefinition.visual.secondary,
                surface = effectiveThemeDefinition.visual.surface,
                surfaceVariant = effectiveThemeDefinition.visual.surfaceVariant,
                onSurface = effectiveThemeDefinition.visual.onSurface,
                onBackground = effectiveThemeDefinition.visual.onBackground,
                outline = effectiveThemeDefinition.visual.borderColor,
            )
        } else {
            baseColorScheme
        }
        adaptedScheme.withSeasonalAccent(seasonalAccent, themeVariant == BrailuxThemeVariant.Dark)
    }
    val colorScheme = if (customBackgroundVisible && !highContrast) {
        themedColorScheme.copy(background = Color.Transparent)
    } else {
        themedColorScheme
    }
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * textSize.scaleFactor,
    )

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalBrailuxTheme provides effectiveThemeDefinition,
        LocalBrailuxStatusColors provides when (themeVariant) {
            BrailuxThemeVariant.Light -> RegularStatusColors
            BrailuxThemeVariant.Dark -> DarkStatusColors
            BrailuxThemeVariant.HighContrast -> HighContrastStatusColors
        },
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BrailuxTypography,
            shapes = BrailuxShapes,
            content = content,
        )
    }
}

@Composable
fun BrailuxPreviewTheme(
    themeDefinition: BrailuxThemeDefinition = BrailuxThemeCatalog.defaultTheme,
    content: @Composable () -> Unit,
) {
    BrailuxAprendeTheme(
        highContrast = false,
        textSize = TextSizePreference.Normal,
        themeDefinition = themeDefinition,
        content = content
    )
}

private data class SeasonalAccentColors(
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
)

private fun androidx.compose.material3.ColorScheme.withSeasonalAccent(
    accent: SeasonalAccent?,
    darkTheme: Boolean,
): androidx.compose.material3.ColorScheme {
    if (accent == null) return this

    val colors = seasonalAccentColors(accent, darkTheme)
    return copy(
        secondary = colors.secondary,
        onSecondary = colors.onSecondary,
        secondaryContainer = colors.secondaryContainer,
        onSecondaryContainer = colors.onSecondaryContainer,
    )
}

private fun seasonalAccentColors(accent: SeasonalAccent, darkTheme: Boolean): SeasonalAccentColors {
    if (darkTheme) {
        return when (accent) {
            SeasonalAccent.Sky -> SeasonalAccentColors(Color(0xFF70C7F2), Color(0xFF003548), Color(0xFF16485D), Color(0xFFC1E9FF))
            SeasonalAccent.Rose -> SeasonalAccentColors(Color(0xFFFFB1C5), Color(0xFF65002E), Color(0xFF7E234A), Color(0xFFFFD9E2))
            SeasonalAccent.Amber -> SeasonalAccentColors(Color(0xFFFFB95C), Color(0xFF492900), Color(0xFF634000), Color(0xFFFFDDB7))
            SeasonalAccent.Red -> SeasonalAccentColors(Color(0xFFFFB4AB), Color(0xFF690005), Color(0xFF7D2521), Color(0xFFFFDAD6))
            SeasonalAccent.Cyan -> SeasonalAccentColors(Color(0xFF53D8EE), Color(0xFF00363D), Color(0xFF174A52), Color(0xFFB7EBF3))
            SeasonalAccent.Orange -> SeasonalAccentColors(Color(0xFFFFB77A), Color(0xFF4B2800), Color(0xFF663D12), Color(0xFFFFDCC3))
            SeasonalAccent.Violet -> SeasonalAccentColors(Color(0xFFCFBCFF), Color(0xFF371E73), Color(0xFF4F378B), Color(0xFFE9DDFF))
            SeasonalAccent.Green -> SeasonalAccentColors(Color(0xFF91D5A2), Color(0xFF00391C), Color(0xFF16512D), Color(0xFFADEFC0))
            SeasonalAccent.Gold -> SeasonalAccentColors(Color(0xFFE9C349), Color(0xFF3B2F00), Color(0xFF554500), Color(0xFFFFE580))
        }
    }

    return when (accent) {
        SeasonalAccent.Sky -> SeasonalAccentColors(Color(0xFF006493), Color.White, Color(0xFFC6E7FF), Color(0xFF001E30))
        SeasonalAccent.Rose -> SeasonalAccentColors(Color(0xFFA33659), Color.White, Color(0xFFFFD9E2), Color(0xFF3F001A))
        SeasonalAccent.Amber -> SeasonalAccentColors(Color(0xFF875400), Color.White, Color(0xFFFFDDB7), Color(0xFF2B1700))
        SeasonalAccent.Red -> SeasonalAccentColors(Color(0xFFB3261E), Color.White, Color(0xFFFFDAD6), Color(0xFF410002))
        SeasonalAccent.Cyan -> SeasonalAccentColors(Color(0xFF00677A), Color.White, Color(0xFFB7EBF3), Color(0xFF001F26))
        SeasonalAccent.Orange -> SeasonalAccentColors(Color(0xFF8C4A00), Color.White, Color(0xFFFFDCC3), Color(0xFF2E1500))
        SeasonalAccent.Violet -> SeasonalAccentColors(Color(0xFF6750A4), Color.White, Color(0xFFE9DDFF), Color(0xFF22005D))
        SeasonalAccent.Green -> SeasonalAccentColors(Color(0xFF2E6B3C), Color.White, Color(0xFFC9EFD1), Color(0xFF00210D))
        SeasonalAccent.Gold -> SeasonalAccentColors(Color(0xFF745B00), Color.White, Color(0xFFFFE580), Color(0xFF241A00))
    }
}
