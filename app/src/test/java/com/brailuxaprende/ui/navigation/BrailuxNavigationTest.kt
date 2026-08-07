package com.brailuxaprende.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailuxNavigationTest {

    @Test
    fun `bottom navigation selects only the active main destination`() {
        assertEquals(BrailuxRoutes.HOME, selectedMainDestination(BrailuxRoutes.HOME))
        assertEquals(BrailuxRoutes.LEARN, selectedMainDestination(BrailuxRoutes.LEARN))
        assertEquals(BrailuxRoutes.PLAY, selectedMainDestination(BrailuxRoutes.PLAY))
        assertEquals(BrailuxRoutes.PROGRESS, selectedMainDestination(BrailuxRoutes.PROGRESS))
    }

    @Test
    fun `settings and about do not select home`() {
        assertNull(selectedMainDestination(BrailuxRoutes.SETTINGS))
        assertNull(selectedMainDestination(BrailuxRoutes.ABOUT))
    }

    @Test
    fun `home navigation does not restore the removed secondary stack`() {
        assertFalse(shouldPreserveMainDestinationState(BrailuxRoutes.HOME))
        assertTrue(shouldPreserveMainDestinationState(BrailuxRoutes.LEARN))
        assertTrue(shouldPreserveMainDestinationState(BrailuxRoutes.PLAY))
        assertTrue(shouldPreserveMainDestinationState(BrailuxRoutes.PROGRESS))
    }
}
