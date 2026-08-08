package com.brailuxaprende.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PracticeDateTest {
    @Test
    fun isoValueRoundTripsAndInvalidDatesAreRejected() {
        val date = PracticeDate(year = 2026, month = 8, day = 8)

        assertEquals("2026-08-08", date.isoValue)
        assertEquals(date, PracticeDate.parse(date.isoValue))
        assertNull(PracticeDate.parse(null))
        assertNull(PracticeDate.parse(""))
        assertNull(PracticeDate.parse("08-08-2026"))
        assertNull(PracticeDate.parse("2026-02-29"))
    }

    @Test
    fun plusDaysHandlesLeapMonthAndYearBoundaries() {
        assertEquals(
            PracticeDate(2024, 2, 29),
            PracticeDate(2024, 2, 28).plusDays(1),
        )
        assertEquals(
            PracticeDate(2024, 3, 1),
            PracticeDate(2024, 2, 29).plusDays(1),
        )
        assertEquals(
            PracticeDate(2026, 3, 1),
            PracticeDate(2026, 2, 28).plusDays(1),
        )
        assertEquals(
            PracticeDate(2027, 1, 1),
            PracticeDate(2026, 12, 31).plusDays(1),
        )
    }

    @Test
    fun weekStartsOnMondayEvenAcrossMonthAndYearBoundaries() {
        val monday = PracticeDate(2025, 12, 29)

        assertEquals(monday, monday.weekStart)
        assertEquals(monday, PracticeDate(2025, 12, 31).weekStart)
        assertEquals(monday, PracticeDate(2026, 1, 1).weekStart)
        assertEquals(monday, PracticeDate(2026, 1, 4).weekStart)
        assertEquals(PracticeDate(2026, 1, 5), PracticeDate(2026, 1, 5).weekStart)
    }

    @Test
    fun monthKeyIncludesTheYear() {
        assertEquals("2026-08", PracticeDate(2026, 8, 31).monthKey)
        assertEquals("2027-08", PracticeDate(2027, 8, 1).monthKey)
    }

    @Test
    fun injectedClockMakesTodayDeterministic() {
        val expected = PracticeDate(2026, 8, 8)
        val clock = PracticeClock { expected }

        assertEquals(expected, clock.today())
    }
}
