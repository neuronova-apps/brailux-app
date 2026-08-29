package com.brailuxaprende

import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.play.GameProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.learning.LearningLesson
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.PermanentAchievement
import com.brailuxaprende.ui.navigation.BrailuxRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailuxAssistantDisabledTest {

    @Test
    fun `assistant feature flag is disabled by default`() {
        assertFalse(
            "The Assistant feature flag must be disabled until a higher quota API is integrated",
            BrailuxFeatures.ASSISTANT_ENABLED,
        )
    }

    @Test
    fun `core application routes remain intact`() {
        assertEquals("inicio", BrailuxRoutes.HOME)
        assertEquals("aprende", BrailuxRoutes.LEARN)
        assertEquals("practica", BrailuxRoutes.PRACTICE)
        assertEquals("juega", BrailuxRoutes.PLAY)
        assertEquals("mi_progreso", BrailuxRoutes.PROGRESS)
        assertEquals("configuracion", BrailuxRoutes.SETTINGS)
        assertEquals("acerca_de", BrailuxRoutes.ABOUT)
        assertEquals("practica_diaria", BrailuxRoutes.DAILY_PRACTICE)
        assertEquals("desafio_del_dia", BrailuxRoutes.DAILY_CHALLENGE)
        assertEquals("practica_explorador_braille", BrailuxRoutes.BRAILLE_EXPLORER)
        assertEquals("practica_reconocedor_braille", BrailuxRoutes.BRAILLE_RECOGNIZER)
        assertEquals("practica_desafio_braille", BrailuxRoutes.BRAILLE_CHALLENGE)
        assertEquals("configuracion_practica_personalizada", BrailuxRoutes.CUSTOM_PRACTICE_CONFIGURATION)
        assertEquals("practica_personalizada", BrailuxRoutes.CUSTOM_PRACTICE)
    }

    @Test
    fun `learning progress operates independently without assistant`() {
        val initialProgress = LearningProgress()
        assertEquals(0, initialProgress.completedLessons.size)
        assertFalse(initialProgress.isCompleted(LearningLesson.SixDots))

        val updatedProgress = LearningProgress(completedLessons = setOf(LearningLesson.SixDots))
        assertTrue(updatedProgress.isCompleted(LearningLesson.SixDots))
        assertFalse(updatedProgress.isCompleted(LearningLesson.Vowels))
    }

    @Test
    fun `practice progress operates independently without assistant`() {
        val initialPractice = PracticeProgress()
        assertEquals(0, initialPractice.level1CompletedSessions)
        assertEquals(0, initialPractice.level1TotalExercises)

        val updatedPractice = PracticeProgress(
            level1CompletedSessions = 1,
            level1TotalExercises = 5,
            level1FirstAttemptCorrect = 4,
            level1Errors = 1,
        )
        assertEquals(1, updatedPractice.level1CompletedSessions)
        assertEquals(5, updatedPractice.level1TotalExercises)
        assertEquals(80, updatedPractice.level1AccuracyPercentage)
    }

    @Test
    fun `game progress operates independently without assistant`() {
        val initialGame = GameProgress()
        assertEquals(0, initialGame.totalGamesCompleted)
        assertEquals(0, initialGame.memoryCompletedGames)

        val updatedGame = GameProgress(
            totalGamesCompleted = 1,
            memoryCompletedGames = 1,
            memoryBestMoves = 12,
        )
        assertEquals(1, updatedGame.totalGamesCompleted)
        assertEquals(1, updatedGame.memoryCompletedGames)
        assertEquals(12, updatedGame.memoryBestMoves)
    }

    @Test
    fun `engagement progress xp streak and achievements operate independently without assistant`() {
        val today = PracticeDate(2026, 8, 29)
        val initialEngagement = EngagementProgress()
        assertEquals(0L, initialEngagement.totalXp)
        assertEquals(0, initialEngagement.currentStreak)

        val withDaily = EngagementProgress(
            totalXp = 50L,
            currentStreak = 1,
            dailyPracticeDates = setOf(today),
            unlockedAchievements = setOf(PermanentAchievement.FirstStep),
        )
        assertTrue(withDaily.isDailyPracticeCompleted(today))
        assertEquals(1, withDaily.currentStreak)
        assertEquals(50L, withDaily.totalXp)
        assertTrue(PermanentAchievement.FirstStep in withDaily.unlockedAchievements)
    }
}
