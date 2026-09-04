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
    fun defaultBackgroundAlwaysUsableAndCannotPreview() {
        val defaultId = BrailuxBackgroundCatalog.DEFAULT_ID
        assertTrue(BrailuxBackgroundCatalog.canUse(defaultId, isPremiumUnlocked = false))
        assertTrue(BrailuxBackgroundCatalog.canSelect(defaultId, isPremiumUnlocked = false))
        assertEquals(false, BrailuxBackgroundCatalog.canPreview(defaultId))
    }

    @Test
    fun allFourPremiumBackgroundsCanPreviewWhileLocked() {
        val premiumIds = listOf(
            BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
        )

        for (id in premiumIds) {
            assertTrue("Should be able to preview $id", BrailuxBackgroundCatalog.canPreview(id))
            assertEquals(
                "Should not be able to use locked $id",
                false,
                BrailuxBackgroundCatalog.canUse(id, isPremiumUnlocked = false),
            )
            assertEquals(
                "Should not be able to select locked $id",
                false,
                BrailuxBackgroundCatalog.canSelect(id, isPremiumUnlocked = false),
            )
        }
    }

    @Test
    fun previewDoesNotUnlockBackground() {
        val id = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID
        val canPreview = BrailuxBackgroundCatalog.canPreview(id)
        assertTrue(canPreview)

        // Previewing does not grant usage right
        val canUse = BrailuxBackgroundCatalog.canUse(id, isPremiumUnlocked = false)
        assertEquals(false, canUse)
    }

    @Test
    fun individuallyAcquiredPremiumCanBeSelectedWhileOthersRemainLocked() {
        val owned = setOf(
            BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
        )

        assertTrue(
            BrailuxBackgroundCatalog.canUse(
                BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
                isPremiumUnlocked = false,
                ownedBackgroundIds = owned,
            ),
        )
        assertTrue(
            BrailuxBackgroundCatalog.canUse(
                BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
                isPremiumUnlocked = false,
                ownedBackgroundIds = owned,
            ),
        )
        assertEquals(
            false,
            BrailuxBackgroundCatalog.canUse(
                BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
                isPremiumUnlocked = false,
                ownedBackgroundIds = owned,
            ),
        )
        assertEquals(
            false,
            BrailuxBackgroundCatalog.canUse(
                BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
                isPremiumUnlocked = false,
                ownedBackgroundIds = owned,
            ),
        )

        val selectedOwned = BrailuxBackgroundCatalog.selectionAfterRequest(
            currentId = BrailuxBackgroundCatalog.DEFAULT_ID,
            requestedId = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            isPremiumUnlocked = false,
            ownedBackgroundIds = owned,
        )
        assertEquals(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID, selectedOwned)

        val selectedUnowned = BrailuxBackgroundCatalog.selectionAfterRequest(
            currentId = BrailuxBackgroundCatalog.DEFAULT_ID,
            requestedId = BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            isPremiumUnlocked = false,
            ownedBackgroundIds = owned,
        )
        assertEquals(BrailuxBackgroundCatalog.DEFAULT_ID, selectedUnowned)
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
