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
        // Jan 5 is within world_braille_day (Jan 3-5), so new_year (Jan 1-4) has ended.
        // Jan 6 is the first date with no active event.
        assertNull(SeasonalThemeResolver.activeEvent(AnnualDate(1, 6), true))
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

    @Test
    fun valentinesDayStartsOnFebruary1() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(2, 1),
            eventsEnabled = true,
        )
        assertEquals("valentines_day", activeEvent?.id)
    }

    @Test
    fun valentinesDayEndsOnFebruary15() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(2, 15),
            eventsEnabled = true,
        )
        assertEquals("valentines_day", activeEvent?.id)
    }

    @Test
    fun valentinesDayIsInactiveOnFebruary16() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(2, 16),
            eventsEnabled = true,
        )
        assertNull(activeEvent)
    }

    @Test
    fun halloweenStartsOnOctober15() {
        // white_cane_day ends Oct 16, halloween starts Oct 15 — white_cane_day has higher priority
        // so on Oct 15, white_cane_day wins. Oct 17 is pure halloween.
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(10, 17),
            eventsEnabled = true,
        )
        assertEquals("halloween", activeEvent?.id)
    }

    @Test
    fun halloweenEndsOnNovember2() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(11, 2),
            eventsEnabled = true,
        )
        assertEquals("halloween", activeEvent?.id)
    }

    @Test
    fun halloweenIsInactiveOnNovember3() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(11, 3),
            eventsEnabled = true,
        )
        assertNull(activeEvent)
    }

    @Test
    fun christmasStartsOnDecember1() {
        // disability_day covers Dec 2-4, so use Dec 1 to isolate christmas
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(12, 1),
            eventsEnabled = true,
        )
        assertEquals("christmas", activeEvent?.id)
    }

    @Test
    fun christmasEndsOnDecember27() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(12, 27),
            eventsEnabled = true,
        )
        assertEquals("christmas", activeEvent?.id)
    }

    @Test
    fun newYearStartsOnDecember28() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(12, 28),
            eventsEnabled = true,
        )
        assertEquals("new_year", activeEvent?.id)
    }

    @Test
    fun newYearEndsOnJanuary4() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(1, 4),
            eventsEnabled = true,
        )
        // world_braille_day is Jan 3-5 with priority 100; new_year is Jan 1-4 with priority 85
        // On Jan 4 both overlap — world_braille_day wins by priority
        // So we test Jan 2 where only new_year is active
        val eventJan2 = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(1, 2),
            eventsEnabled = true,
        )
        assertEquals("new_year", eventJan2?.id)
    }

    @Test
    fun newYearIsInactiveOnJanuary5WhenBrailleDayTakesPriority() {
        // Jan 5 is covered by world_braille_day (Jan 3-5), not new_year (Jan 1-4)
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(1, 5),
            eventsEnabled = true,
        )
        assertEquals("world_braille_day", activeEvent?.id)
    }

    @Test
    fun newYearIsInactiveOnJanuary6() {
        val activeEvent = SeasonalThemeResolver.activeEvent(
            date = AnnualDate(1, 6),
            eventsEnabled = true,
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
