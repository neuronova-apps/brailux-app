package com.brailuxaprende.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressPresentationTest {
    @Test
    fun `accuracy uses real accumulated counters`() {
        assertEquals(75, accuracyPercentage(firstAttemptCorrect = 9, totalExercises = 12))
        assertEquals(80, accuracyPercentage(firstAttemptCorrect = 24, totalExercises = 30))
        assertEquals(90, accuracyPercentage(firstAttemptCorrect = 18, totalExercises = 20))
    }

    @Test
    fun `accuracy is safe for empty or inconsistent counters`() {
        assertEquals(0, accuracyPercentage(firstAttemptCorrect = 0, totalExercises = 0))
        assertEquals(100, accuracyPercentage(firstAttemptCorrect = 30, totalExercises = 20))
    }
}
