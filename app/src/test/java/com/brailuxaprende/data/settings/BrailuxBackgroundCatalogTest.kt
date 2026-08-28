package com.brailuxaprende.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailuxBackgroundCatalogTest {
    @Test
    fun premiumBackgroundDoesNotChangeSelectionWhenPremiumIsLocked() {
        val selectedId = BrailuxBackgroundCatalog.selectionAfterRequest(
            currentId = BrailuxBackgroundCatalog.DEFAULT_ID,
            requestedId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            isPremiumUnlocked = false,
        )

        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, selectedId)
    }

    @Test
    fun premiumBackgroundCanBeSelectedWhenPremiumIsUnlocked() {
        val selectedId = BrailuxBackgroundCatalog.selectionAfterRequest(
            currentId = BrailuxBackgroundCatalog.DEFAULT_ID,
            requestedId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            isPremiumUnlocked = true,
        )

        assertEquals(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID, selectedId)
    }

    @Test
    fun allCustomBackgroundsArePremiumAndDefaultIsFree() {
        val defaultBackground = BrailuxBackgroundCatalog.option(
            BrailuxBackgroundCatalog.DEFAULT_ID,
        )
        val customBackgrounds = BrailuxBackgroundCatalog.backgrounds.filter {
            it.id != BrailuxBackgroundCatalog.DEFAULT_ID
        }

        assertEquals(false, defaultBackground?.premium)
        assertTrue(customBackgrounds.all { it.premium })
    }

    @Test
    fun highContrastHidesBackgroundWithoutChangingSelectedId() {
        val selectedId = BrailuxBackgroundCatalog.CREMA_ONDAS_ID

        val drawable = BrailuxBackgroundCatalog.activeDrawableResource(
            selectedId = selectedId,
            isPremiumUnlocked = true,
            highContrastEnabled = true,
        )

        assertNull(drawable)
        assertEquals(BrailuxBackgroundCatalog.CREMA_ONDAS_ID, selectedId)
    }

    @Test
    fun defaultRestoresNormalAppearanceWithoutImage() {
        val drawable = BrailuxBackgroundCatalog.activeDrawableResource(
            selectedId = BrailuxBackgroundCatalog.DEFAULT_ID,
            isPremiumUnlocked = true,
            highContrastEnabled = false,
        )

        assertNull(drawable)
    }
}
