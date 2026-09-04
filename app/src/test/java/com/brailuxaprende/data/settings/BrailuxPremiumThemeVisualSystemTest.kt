package com.brailuxaprende.data.settings

import androidx.compose.ui.graphics.Color
import com.brailuxaprende.R
import com.brailuxaprende.ui.theme.BRAILLE_GEOMETRIC_ACCENT
import com.brailuxaprende.ui.theme.BRAILLE_MIST_ACCENT
import com.brailuxaprende.ui.theme.BRAILLE_ORGANIC_ACCENT
import com.brailuxaprende.ui.theme.BRAILLE_TACTILE_WAVE_ACCENT
import com.brailuxaprende.ui.theme.BrailuxBlue
import com.brailuxaprende.ui.theme.BrailuxThemeCatalog
import com.brailuxaprende.ui.theme.HighContrastBackground
import com.brailuxaprende.ui.theme.HighContrastPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailuxPremiumThemeVisualSystemTest {

    // 1. Default conserva visual predeterminado.
    @Test
    fun defaultConservaVisualPredeterminado() {
        val theme = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.DEFAULT_ID,
            isPremiumUnlocked = false,
        )

        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, theme.id)
        assertNull(theme.backgroundRes)
        assertFalse(theme.premium)
        assertNull(theme.accentStyle)
        assertNull(theme.headerDecorationRes)
        assertNull(theme.cardDecorationRes)
        assertEquals(BrailuxBlue, theme.visual.primary)
        assertEquals(BrailuxBlue, theme.visual.buttonColor)
        assertEquals(Color.White, theme.visual.onButtonColor)
    }

    // 2. Cada ID Premium resuelve su definición visual correcta.
    @Test
    fun cadaIdPremiumResuelveSuDefinicionVisualCorrecta() {
        val celeste = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            isPremiumUnlocked = true,
        )
        assertEquals(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID, celeste.id)
        assertEquals(R.drawable.bg_brailux_celeste_geometrico, celeste.backgroundRes)
        assertTrue(celeste.premium)
        assertEquals(BRAILLE_GEOMETRIC_ACCENT, celeste.accentStyle)
        assertEquals(Color(0xFF0277BD), celeste.visual.primary)

        val crema = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            isPremiumUnlocked = true,
        )
        assertEquals(BrailuxBackgroundCatalog.CREMA_ONDAS_ID, crema.id)
        assertEquals(R.drawable.bg_brailux_crema_ondas, crema.backgroundRes)
        assertTrue(crema.premium)
        assertEquals(BRAILLE_TACTILE_WAVE_ACCENT, crema.accentStyle)
        assertEquals(Color(0xFF8D5B28), crema.visual.primary)

        val lavanda = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            isPremiumUnlocked = true,
        )
        assertEquals(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID, lavanda.id)
        assertEquals(R.drawable.bg_brailux_lavanda_niebla, lavanda.backgroundRes)
        assertTrue(lavanda.premium)
        assertEquals(BRAILLE_MIST_ACCENT, lavanda.accentStyle)
        assertEquals(Color(0xFF5E4996), lavanda.visual.primary)

        val salvia = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            isPremiumUnlocked = true,
        )
        assertEquals(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID, salvia.id)
        assertEquals(R.drawable.bg_brailux_salvia_textura, salvia.backgroundRes)
        assertTrue(salvia.premium)
        assertEquals(BRAILLE_ORGANIC_ACCENT, salvia.accentStyle)
        assertEquals(Color(0xFF33694E), salvia.visual.primary)
    }

    // 3. Fondo Premium bloqueado no activa su paquete visual.
    @Test
    fun fondoPremiumBloqueadoNoActivaSuPaqueteVisual() {
        val theme = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            isPremiumUnlocked = false,
            ownedBackgroundIds = emptySet(),
        )

        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, theme.id)
        assertNull(theme.backgroundRes)
        assertFalse(theme.premium)
        assertNull(theme.accentStyle)
        assertEquals(BrailuxThemeCatalog.defaultVisual, theme.visual)
    }

    // 4. Fondo Premium adquirido activa su paquete visual.
    @Test
    fun fondoPremiumAdquiridoActivaSuPaqueteVisual() {
        val ownedIds = setOf(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID)

        val acquiredTheme = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            isPremiumUnlocked = false,
            ownedBackgroundIds = ownedIds,
        )
        assertEquals(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID, acquiredTheme.id)
        assertEquals(BRAILLE_ORGANIC_ACCENT, acquiredTheme.accentStyle)
        assertEquals(Color(0xFF33694E), acquiredTheme.visual.primary)

        val lockedTheme = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            isPremiumUnlocked = false,
            ownedBackgroundIds = ownedIds,
        )
        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, lockedTheme.id)
        assertNull(lockedTheme.backgroundRes)
    }

    // 5. La rotación cambia definición temática completa.
    @Test
    fun laRotacionCambiaDefinicionTematicaCompleta() {
        val initialId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID
        val nextId = BrailuxBackgroundRotationPolicy.nextPremiumBackgroundId(
            currentId = initialId,
            isPremiumUnlocked = true,
        )
        assertNotNull(nextId)

        val initialTheme = BrailuxThemeCatalog.resolveTheme(initialId, isPremiumUnlocked = true)
        val nextTheme = BrailuxThemeCatalog.resolveTheme(nextId, isPremiumUnlocked = true)

        assertEquals(BRAILLE_GEOMETRIC_ACCENT, initialTheme.accentStyle)
        assertEquals(BRAILLE_TACTILE_WAVE_ACCENT, nextTheme.accentStyle)
        assertFalse(initialTheme.visual.primary == nextTheme.visual.primary)
    }

    // 6. Rotación solo usa IDs adquiridos.
    @Test
    fun rotacionSoloUsaIdsAdquiridos() {
        val owned = setOf(
            BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
        )

        val next1 = BrailuxBackgroundRotationPolicy.nextPremiumBackgroundId(
            currentId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            isPremiumUnlocked = false,
            ownedBackgroundIds = owned,
        )
        assertEquals(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID, next1)

        val next2 = BrailuxBackgroundRotationPolicy.nextPremiumBackgroundId(
            currentId = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            isPremiumUnlocked = false,
            ownedBackgroundIds = owned,
        )
        assertEquals(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID, next2)
    }

    // 7. Alto contraste anula/suprime recursos visuales incompatibles.
    @Test
    fun altoContrasteAnulaSuprimeRecursosVisualesIncompatibles() {
        val theme = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            isPremiumUnlocked = true,
            highContrastEnabled = true,
        )

        assertEquals("high_contrast", theme.id)
        assertNull(theme.backgroundRes)
        assertNull(theme.accentStyle)
        assertNull(theme.headerDecorationRes)
        assertNull(theme.cardDecorationRes)
        assertEquals(HighContrastPrimary, theme.visual.primary)
        assertEquals(HighContrastBackground, theme.visual.surface)
    }

    // 8. Tema estacional conserva prioridad.
    @Test
    fun temaEstacionalConservaPrioridad() {
        val theme = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            isPremiumUnlocked = true,
            seasonalThemeActive = true,
        )

        // The underlying theme identity is preserved
        assertEquals(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID, theme.id)
        // Background and decorative assets are suppressed to yield priority to seasonal resources
        assertNull(theme.backgroundRes)
        assertNull(theme.headerDecorationRes)
        assertNull(theme.cardDecorationRes)

        // When seasonal theme is inactive, full theme visuals are returned
        val restored = BrailuxThemeCatalog.resolveTheme(
            selectedId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            isPremiumUnlocked = true,
            seasonalThemeActive = false,
        )
        assertEquals(R.drawable.bg_brailux_celeste_geometrico, restored.backgroundRes)
    }

    // 9. Null en decoraciones no provoca crash.
    @Test
    fun nullEnDecoracionesNoProvocaCrash() {
        BrailuxThemeCatalog.themes.forEach { theme ->
            assertNull("headerDecorationRes should be null for ${theme.id}", theme.headerDecorationRes)
            assertNull("cardDecorationRes should be null for ${theme.id}", theme.cardDecorationRes)
            assertNotNull("visual should be present for ${theme.id}", theme.visual)
        }
    }

    // 10. Preview bloqueada no concede propiedad.
    @Test
    fun previewBloqueadaNoConcedePropiedad() {
        val id = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID
        assertTrue(BrailuxBackgroundCatalog.canPreview(id))
        assertFalse(BrailuxBackgroundCatalog.canUse(id, isPremiumUnlocked = false))
        assertFalse(BrailuxBackgroundCatalog.canSelect(id, isPremiumUnlocked = false))

        val resolved = BrailuxThemeCatalog.resolveTheme(id, isPremiumUnlocked = false)
        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, resolved.id)
    }

    // 11. Default continúa sin preview.
    @Test
    fun defaultContinuaSinPreview() {
        assertFalse(BrailuxBackgroundCatalog.canPreview(BrailuxBackgroundCatalog.DEFAULT_ID))
    }

    // 12. Los cuatro Premium continúan con preview.
    @Test
    fun losCuatroPremiumContinuanConPreview() {
        val premiumIds = listOf(
            BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
        )
        for (id in premiumIds) {
            assertTrue("Theme $id must allow preview", BrailuxBackgroundCatalog.canPreview(id))
        }
    }

    // 13. No se rompe normalización de IDs desconocidos.
    @Test
    fun noSeRompeNormalizacionDeIdsDesconocidos() {
        val unknownId = "unknown_random_theme_123"
        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, BrailuxBackgroundCatalog.normalizedId(unknownId))
        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, BrailuxBackgroundCatalog.normalizedId(null))

        val themeForUnknown = BrailuxThemeCatalog.resolveTheme(unknownId, isPremiumUnlocked = true)
        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, themeForUnknown.id)

        val themeForNull = BrailuxThemeCatalog.resolveTheme(null, isPremiumUnlocked = true)
        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, themeForNull.id)
    }

    // 14. Persistencia existente continúa siendo compatible.
    @Test
    fun persistenciaExistenteContinuaSiendoCompatible() {
        val defaultPreferences = AccessibilityPreferences()
        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, defaultPreferences.selectedBackgroundId)
        assertEquals(BackgroundRotationMode.Fixed, defaultPreferences.backgroundRotationMode)

        val customPreferences = defaultPreferences.copy(
            selectedBackgroundId = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            backgroundRotationMode = BackgroundRotationMode.OnAppOpen,
        )
        assertEquals(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID, customPreferences.selectedBackgroundId)
        assertEquals(BackgroundRotationMode.OnAppOpen, customPreferences.backgroundRotationMode)

        val resolved = BrailuxThemeCatalog.resolveTheme(
            selectedId = customPreferences.selectedBackgroundId,
            isPremiumUnlocked = true,
        )
        assertEquals(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID, resolved.id)
    }
}
