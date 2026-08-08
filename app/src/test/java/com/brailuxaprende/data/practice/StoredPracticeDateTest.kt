package com.brailuxaprende.data.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoredPracticeDateTest {
    @Test
    fun parsesPersistedIsoDate() {
        assertEquals(
            StoredPracticeDate(year = 2026, month = 8, day = 7),
            parseStoredPracticeDate("2026-08-07"),
        )
    }

    @Test
    fun rejectsMissingOrInvalidPersistedDates() {
        assertNull(parseStoredPracticeDate(null))
        assertNull(parseStoredPracticeDate(""))
        assertNull(parseStoredPracticeDate("7 ago. 2026"))
        assertNull(parseStoredPracticeDate("2026-02-29"))
    }

    @Test
    fun acceptsLeapDay() {
        assertEquals(
            StoredPracticeDate(year = 2024, month = 2, day = 29),
            parseStoredPracticeDate("2024-02-29"),
        )
    }
}
