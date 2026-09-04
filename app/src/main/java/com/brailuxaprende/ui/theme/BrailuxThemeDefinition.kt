package com.brailuxaprende.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.brailuxaprende.R
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog

const val BRAILLE_GEOMETRIC_ACCENT = "BRAILLE_GEOMETRIC_ACCENT"
const val BRAILLE_TACTILE_WAVE_ACCENT = "BRAILLE_TACTILE_WAVE_ACCENT"
const val BRAILLE_MIST_ACCENT = "BRAILLE_MIST_ACCENT"
const val BRAILLE_ORGANIC_ACCENT = "BRAILLE_ORGANIC_ACCENT"

@Immutable
data class BrailuxThemeVisualDefinition(
    val primary: Color,
    val secondary: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onBackground: Color,
    val cardColor: Color,
    val borderColor: Color,
    val iconTint: Color,
    val buttonColor: Color,
    val onButtonColor: Color,
    val progressColor: Color,
    val chipColor: Color,
    val accentAlpha: Float,
)

@Immutable
data class BrailuxThemeDefinition(
    val id: String,
    @param:DrawableRes val backgroundRes: Int?,
    val premium: Boolean,
    val visual: BrailuxThemeVisualDefinition,
    @param:DrawableRes val headerDecorationRes: Int? = null,
    @param:DrawableRes val cardDecorationRes: Int? = null,
    val accentStyle: String? = null,
)

object BrailuxThemeCatalog {

    val defaultVisual = BrailuxThemeVisualDefinition(
        primary = BrailuxBlue,
        secondary = Color(0xFF1769AA),
        surface = BrailuxSurface,
        surfaceVariant = BrailuxSurfaceVariant,
        onSurface = BrailuxText,
        onBackground = BrailuxText,
        cardColor = BrailuxSurface,
        borderColor = Color(0xFFC0CDDA),
        iconTint = BrailuxBlue,
        buttonColor = BrailuxBlue,
        onButtonColor = Color.White,
        progressColor = BrailuxBlue,
        chipColor = Color(0xFFD8EBFF),
        accentAlpha = 1.0f,
    )

    val defaultTheme = BrailuxThemeDefinition(
        id = BrailuxBackgroundCatalog.DEFAULT_ID,
        backgroundRes = null,
        premium = false,
        visual = defaultVisual,
        headerDecorationRes = null,
        cardDecorationRes = null,
        accentStyle = null,
    )

    val celesteGeometricoVisual = BrailuxThemeVisualDefinition(
        primary = Color(0xFF0277BD),
        secondary = Color(0xFF00838F),
        surface = Color(0xFFF2F8FD),
        surfaceVariant = Color(0xFFE1EFFB),
        onSurface = Color(0xFF0A2540),
        onBackground = Color(0xFF0A2540),
        cardColor = Color(0xFFF8FBFE),
        borderColor = Color(0xFF81D4FA),
        iconTint = Color(0xFF0277BD),
        buttonColor = Color(0xFF0277BD),
        onButtonColor = Color.White,
        progressColor = Color(0xFF0288D1),
        chipColor = Color(0xFFD3EBFB),
        accentAlpha = 0.95f,
    )

    val celesteGeometricoTheme = BrailuxThemeDefinition(
        id = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
        backgroundRes = R.drawable.bg_brailux_celeste_geometrico,
        premium = true,
        visual = celesteGeometricoVisual,
        headerDecorationRes = null,
        cardDecorationRes = null,
        accentStyle = BRAILLE_GEOMETRIC_ACCENT,
    )

    val cremaOndasVisual = BrailuxThemeVisualDefinition(
        primary = Color(0xFF8D5B28),
        secondary = Color(0xFFA06D3B),
        surface = Color(0xFFFDFBF7),
        surfaceVariant = Color(0xFFF4ECE1),
        onSurface = Color(0xFF2C2218),
        onBackground = Color(0xFF2C2218),
        cardColor = Color(0xFFFFFDF9),
        borderColor = Color(0xFFD7C4AE),
        iconTint = Color(0xFF8D5B28),
        buttonColor = Color(0xFF8D5B28),
        onButtonColor = Color(0xFFFFFDF9),
        progressColor = Color(0xFF8D5B28),
        chipColor = Color(0xFFEFE2D1),
        accentAlpha = 0.90f,
    )

    val cremaOndasTheme = BrailuxThemeDefinition(
        id = BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
        backgroundRes = R.drawable.bg_brailux_crema_ondas,
        premium = true,
        visual = cremaOndasVisual,
        headerDecorationRes = null,
        cardDecorationRes = null,
        accentStyle = BRAILLE_TACTILE_WAVE_ACCENT,
    )

    val lavandaNieblaVisual = BrailuxThemeVisualDefinition(
        primary = Color(0xFF5E4996),
        secondary = Color(0xFF7A68A6),
        surface = Color(0xFFF9F7FD),
        surfaceVariant = Color(0xFFEDE7F6),
        onSurface = Color(0xFF231B38),
        onBackground = Color(0xFF231B38),
        cardColor = Color(0xFFFCFAFF),
        borderColor = Color(0xFFCABEE6),
        iconTint = Color(0xFF5E4996),
        buttonColor = Color(0xFF5E4996),
        onButtonColor = Color.White,
        progressColor = Color(0xFF673AB7),
        chipColor = Color(0xFFE7DEF7),
        accentAlpha = 0.85f,
    )

    val lavandaNieblaTheme = BrailuxThemeDefinition(
        id = BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
        backgroundRes = R.drawable.bg_brailux_lavanda_niebla,
        premium = true,
        visual = lavandaNieblaVisual,
        headerDecorationRes = null,
        cardDecorationRes = null,
        accentStyle = BRAILLE_MIST_ACCENT,
    )

    val salviaTexturaVisual = BrailuxThemeVisualDefinition(
        primary = Color(0xFF33694E),
        secondary = Color(0xFF4E7D63),
        surface = Color(0xFFF6FAF7),
        surfaceVariant = Color(0xFFE5EFE7),
        onSurface = Color(0xFF162B1F),
        onBackground = Color(0xFF162B1F),
        cardColor = Color(0xFFFAFDFB),
        borderColor = Color(0xFFA5C9B3),
        iconTint = Color(0xFF33694E),
        buttonColor = Color(0xFF33694E),
        onButtonColor = Color.White,
        progressColor = Color(0xFF33694E),
        chipColor = Color(0xFFDCEADF),
        accentAlpha = 0.90f,
    )

    val salviaTexturaTheme = BrailuxThemeDefinition(
        id = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
        backgroundRes = R.drawable.bg_brailux_salvia_textura,
        premium = true,
        visual = salviaTexturaVisual,
        headerDecorationRes = null,
        cardDecorationRes = null,
        accentStyle = BRAILLE_ORGANIC_ACCENT,
    )

    val highContrastVisual = BrailuxThemeVisualDefinition(
        primary = HighContrastPrimary,
        secondary = HighContrastSecondary,
        surface = HighContrastBackground,
        surfaceVariant = HighContrastSurfaceVariant,
        onSurface = HighContrastForeground,
        onBackground = HighContrastForeground,
        cardColor = HighContrastBackground,
        borderColor = HighContrastForeground,
        iconTint = HighContrastPrimary,
        buttonColor = HighContrastPrimary,
        onButtonColor = HighContrastBackground,
        progressColor = HighContrastPrimary,
        chipColor = HighContrastSurfaceVariant,
        accentAlpha = 1.0f,
    )

    val highContrastTheme = BrailuxThemeDefinition(
        id = "high_contrast",
        backgroundRes = null,
        premium = false,
        visual = highContrastVisual,
        headerDecorationRes = null,
        cardDecorationRes = null,
        accentStyle = null,
    )

    val themes: List<BrailuxThemeDefinition> = listOf(
        defaultTheme,
        celesteGeometricoTheme,
        cremaOndasTheme,
        lavandaNieblaTheme,
        salviaTexturaTheme,
    )

    fun theme(id: String?): BrailuxThemeDefinition? =
        themes.firstOrNull { it.id == id }

    fun resolveTheme(
        selectedId: String?,
        isPremiumUnlocked: Boolean = false,
        ownedBackgroundIds: Set<String> = emptySet(),
        highContrastEnabled: Boolean = false,
        seasonalThemeActive: Boolean = false,
    ): BrailuxThemeDefinition {
        if (highContrastEnabled) {
            return highContrastTheme
        }

        val normalizedId = BrailuxBackgroundCatalog.normalizedId(selectedId)
        val candidate = theme(normalizedId) ?: defaultTheme

        val canUseCandidate = if (!candidate.premium) {
            true
        } else {
            BrailuxBackgroundCatalog.canUse(
                id = candidate.id,
                isPremiumUnlocked = isPremiumUnlocked,
                ownedBackgroundIds = ownedBackgroundIds,
            )
        }

        val effectiveTheme = if (canUseCandidate) candidate else defaultTheme

        if (seasonalThemeActive) {
            return effectiveTheme.copy(
                backgroundRes = null,
                headerDecorationRes = null,
                cardDecorationRes = null,
            )
        }

        return effectiveTheme
    }
}

val LocalBrailuxTheme = staticCompositionLocalOf { BrailuxThemeCatalog.defaultTheme }
