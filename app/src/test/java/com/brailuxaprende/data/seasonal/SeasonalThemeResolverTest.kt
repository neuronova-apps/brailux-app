package com.brailuxaprende.data.seasonal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeasonalThemeResolverTest {
    @Test
    fun eventIsActiveInsideItsWindow() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(1, 4),
            eventsEnabled = true,
        )

        assertEquals("world_braille_day", activeEvent?.id)
    }

    @Test
    fun eventIsInactiveOutsideItsWindow() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(1, 6),
            eventsEnabled = true,
        )

        assertNull(activeEvent)
    }

    @Test
    fun newYearWindowCrossesTheEndOfTheYear() {
        assertEquals(
            "new_year",
            SeasonalThemeResolver.activeEvent(AnnualDate(12, 31), true)?.id,
        )
        assertEquals(
            "new_year",
            SeasonalThemeResolver.activeEvent(AnnualDate(1, 1), true)?.id,
        )
        assertNull(SeasonalThemeResolver.activeEvent(AnnualDate(1, 2), true))
    }

    @Test
    fun highestPriorityEventWinsWhenWindowsOverlap() {
        val lowerPriority = testEvent(id = "lower", priority = 10)
        val higherPriority = testEvent(id = "higher", priority = 20)

        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(6, 11),
            eventsEnabled = true,
            events = listOf(lowerPriority, higherPriority),
        )

        assertEquals("higher", activeEvent?.id)
    }

    @Test
    fun disabledEventsAreIgnored() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(12, 24),
            eventsEnabled = false,
        )

        assertNull(activeEvent)
    }

    private fun testEvent(id: String, priority: Int): SeasonalEvent = SeasonalEvent(
        id = id,
        nameResource = 0,
        start = AnnualDate(6, 10),
        end = AnnualDate(6, 12),
        priority = priority,
        visualState = SeasonalVisualState.Braille,
    )
}
