package com.brailuxaprende.ui.theme

import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.data.settings.AppearancePreference
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceThemeResolverTest {
    @Test
    fun defaultPreferenceUsesLightAppearance() {
        assertEquals(AppearancePreference.Light, AccessibilityPreferences().appearance)
        assertEquals(AppearancePreference.Light, AppearancePreference.fromStoredValue(null))
    }

    @Test
    fun followSystemUsesTheCurrentSystemAppearance() {
        assertEquals(
            BrailuxThemeVariant.Light,
            resolveThemeVariant(AppearancePreference.System, false, false),
        )
        assertEquals(
            BrailuxThemeVariant.Dark,
            resolveThemeVariant(AppearancePreference.System, true, false),
        )
    }

    @Test
    fun explicitAppearanceOverridesTheSystemAppearance() {
        assertEquals(
            BrailuxThemeVariant.Light,
            resolveThemeVariant(AppearancePreference.Light, true, false),
        )
        assertEquals(
            BrailuxThemeVariant.Dark,
            resolveThemeVariant(AppearancePreference.Dark, false, false),
        )
    }

    @Test
    fun highContrastHasVisualPriorityForEveryAppearance() {
        AppearancePreference.entries.forEach { appearance ->
            assertEquals(
                BrailuxThemeVariant.HighContrast,
                resolveThemeVariant(appearance, systemInDarkTheme = false, highContrast = true),
            )
            assertEquals(
                BrailuxThemeVariant.HighContrast,
                resolveThemeVariant(appearance, systemInDarkTheme = true, highContrast = true),
            )
        }
    }
}
