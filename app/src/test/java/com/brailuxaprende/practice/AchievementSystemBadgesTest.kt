package com.brailuxaprende.practice

import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.learning.LearningLesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementSystemBadgesTest {

    @Test
    fun exactamente25InsigniasActivasEn5Familias() {
        val active = PermanentAchievement.activeEntries
        assertEquals(25, active.size)

        val byFamily = active.groupBy { it.family }
        assertEquals(5, byFamily[AchievementFamily.LearningPath]?.size)
        assertEquals(6, byFamily[AchievementFamily.Consistency]?.size)
        assertEquals(6, byFamily[AchievementFamily.BrailleTrajectory]?.size)
        assertEquals(6, byFamily[AchievementFamily.Precision]?.size)
        assertEquals(2, byFamily[AchievementFamily.MixedMastery]?.size)

        assertTrue(PermanentAchievement.HundredExercises.isLegacy)
        assertFalse(PermanentAchievement.HundredExercises in active)
    }

    @Test
    fun familiaCaminoDeAprendizaje_seDesbloqueaConformeALosRequisitos() {
        val base = EngagementProgress()

        // 1. Primer paso: 1 sesión de práctica
        val step1 = EngagementEngine.evaluateAchievements(base.copy(totalSessions = 1))
        assertTrue(PermanentAchievement.FirstStep in step1)

        // 2. Explorador Braille: 5 sesiones de Nivel 1
        val explorerUnder = EngagementEngine.evaluateAchievements(base.copy(level1Sessions = 4))
        assertFalse(PermanentAchievement.Explorer in explorerUnder)
        val explorerDone = EngagementEngine.evaluateAchievements(base.copy(level1Sessions = 5))
        assertTrue(PermanentAchievement.Explorer in explorerDone)

        // 3. Reconocedor Braille: 5 sesiones de Nivel 2
        val recognizerUnder = EngagementEngine.evaluateAchievements(base.copy(level2Sessions = 4))
        assertFalse(PermanentAchievement.Recognizer in recognizerUnder)
        val recognizerDone = EngagementEngine.evaluateAchievements(base.copy(level2Sessions = 5))
        assertTrue(PermanentAchievement.Recognizer in recognizerDone)

        // 4. Desafiante Braille: 3 sesiones de Nivel 3
        val challengerUnder = EngagementEngine.evaluateAchievements(base.copy(level3Sessions = 2))
        assertFalse(PermanentAchievement.Challenger in challengerUnder)
        val challengerDone = EngagementEngine.evaluateAchievements(base.copy(level3Sessions = 3))
        assertTrue(PermanentAchievement.Challenger in challengerDone)

        // 5. Alfabeto completo: 5 lecciones de Aprende
        val partialLearn4 = LearningProgress(
            completedLessons = setOf(
                LearningLesson.SixDots,
                LearningLesson.Vowels,
                LearningLesson.LettersAtoJ,
                LearningLesson.LettersKtoT,
            ),
        )
        val fullAlphabetUnder4 = EngagementEngine.evaluateAchievements(base, partialLearn4)
        assertFalse("Caso A: 4 lecciones completadas no deben desbloquear FullAlphabet", PermanentAchievement.FullAlphabet in fullAlphabetUnder4)

        val fullLearn5 = LearningProgress(completedLessons = LearningLesson.entries.toSet())
        val fullAlphabetDone5 = EngagementEngine.evaluateAchievements(base, fullLearn5)
        assertTrue("Caso B: 5 lecciones completadas deben desbloquear FullAlphabet", PermanentAchievement.FullAlphabet in fullAlphabetDone5)

        // Caso C: 5 lecciones completadas con errores registrados
        val baseWithErrors = base.copy(
            totalExercises = 50,
            totalSessions = 5,
        )
        val sessionWithErrors = EngagementSession(
            id = "session_with_errors",
            kind = PracticeSessionKind.Daily,
            exercisesCompleted = 5,
            firstAttemptCorrect = 3,
            errors = 7,
            longestFirstAttemptCorrectStreak = 1,
        )
        val recordWithErrors = EngagementEngine.recordSession(
            current = baseWithErrors,
            session = sessionWithErrors,
            date = PracticeDate(2026, 8, 30),
            learningProgress = fullLearn5,
        )
        assertTrue(
            "Caso C: 5 lecciones completadas con errores registrados deben desbloquear FullAlphabet igualmente",
            PermanentAchievement.FullAlphabet in recordWithErrors.progress.unlockedAchievements,
        )
    }

    @Test
    fun familiaConstancia_seDesbloqueaConDiasYRachasReales() {
        val base = EngagementProgress()

        // 1. Constancia: 3 días distintos
        val d1 = PracticeDate(2026, 8, 1)
        val d2 = PracticeDate(2026, 8, 3)
        val d3 = PracticeDate(2026, 8, 5)
        val consistencyUnder = EngagementEngine.evaluateAchievements(base.copy(activityDates = setOf(d1, d2)))
        assertFalse(PermanentAchievement.Consistency in consistencyUnder)
        val consistencyDone = EngagementEngine.evaluateAchievements(base.copy(activityDates = setOf(d1, d2, d3)))
        assertTrue(PermanentAchievement.Consistency in consistencyDone)

        // 2. Semana en movimiento: 5 días en la misma semana
        val mon = PracticeDate(2026, 8, 3)
        val splitDays = setOf(mon.plusDays(4), mon.plusDays(5), mon.plusDays(6), mon.plusDays(7), mon.plusDays(8))
        val weekUnder = EngagementEngine.evaluateAchievements(base.copy(activityDates = splitDays))
        assertFalse(PermanentAchievement.WeekInMotion in weekUnder)

        val sameWeekDays = (0..4).map { mon.plusDays(it) }.toSet()
        val weekDone = EngagementEngine.evaluateAchievements(base.copy(activityDates = sameWeekDays))
        assertTrue(PermanentAchievement.WeekInMotion in weekDone)

        // 3. Semana constante: Racha 7 días
        val streak6 = EngagementEngine.evaluateAchievements(base.copy(bestStreak = 6))
        assertFalse(PermanentAchievement.ConstantWeek in streak6)
        val streak7 = EngagementEngine.evaluateAchievements(base.copy(bestStreak = 7))
        assertTrue(PermanentAchievement.ConstantWeek in streak7)

        // 4. Dos semanas: Racha 14 días
        val streak13 = EngagementEngine.evaluateAchievements(base.copy(bestStreak = 13))
        assertFalse(PermanentAchievement.TwoWeeks in streak13)
        val streak14 = EngagementEngine.evaluateAchievements(base.copy(bestStreak = 14))
        assertTrue(PermanentAchievement.TwoWeeks in streak14)

        // 5. Mes de constancia: Racha 30 días
        val streak29 = EngagementEngine.evaluateAchievements(base.copy(bestStreak = 29))
        assertFalse(PermanentAchievement.ConsistencyMonth in streak29)
        val streak30 = EngagementEngine.evaluateAchievements(base.copy(bestStreak = 30))
        assertTrue(PermanentAchievement.ConsistencyMonth in streak30)

        // 6. Constancia superior: Racha 60 días
        val streak59 = EngagementEngine.evaluateAchievements(base.copy(bestStreak = 59))
        assertFalse(PermanentAchievement.SuperiorConsistency in streak59)
        val streak60 = EngagementEngine.evaluateAchievements(base.copy(bestStreak = 60))
        assertTrue(PermanentAchievement.SuperiorConsistency in streak60)
    }

    @Test
    fun familiaTrayectoriaBraille_desbloqueaPorEjerciciosAcumulados() {
        val base = EngagementProgress()

        val thresholds = listOf(
            24L to 25L to PermanentAchievement.Bronze,
            74L to 75L to PermanentAchievement.Silver,
            124L to 125L to PermanentAchievement.Gold,
            299L to 300L to PermanentAchievement.Platinum,
            599L to 600L to PermanentAchievement.Diamond,
            1199L to 1200L to PermanentAchievement.BrailleSupremacy,
        )

        for ((pair, achievement) in thresholds) {
            val (underVal, doneVal) = pair
            val under = EngagementEngine.evaluateAchievements(base.copy(totalExercises = underVal))
            assertFalse("Expected not unlocked at $underVal for $achievement", achievement in under)

            val done = EngagementEngine.evaluateAchievements(base.copy(totalExercises = doneVal))
            assertTrue("Expected unlocked at $doneVal for $achievement", achievement in done)
        }
    }

    @Test
    fun familiaPrecision_desbloqueaPorMejorRachaDePrecision() {
        val base = EngagementProgress()

        val thresholds = listOf(
            4 to 5 to PermanentAchievement.BrailleFocus,
            9 to 10 to PermanentAchievement.BrailleRhythm,
            14 to 15 to PermanentAchievement.BraillePrecision,
            29 to 30 to PermanentAchievement.SustainedReading,
            49 to 50 to PermanentAchievement.ConstantMastery,
            74 to 75 to PermanentAchievement.SuperiorPrecision,
        )

        for ((pair, achievement) in thresholds) {
            val (underVal, doneVal) = pair
            val under = EngagementEngine.evaluateAchievements(base.copy(bestPrecisionStreak = underVal))
            assertFalse("Expected not unlocked at $underVal for $achievement", achievement in under)

            val done = EngagementEngine.evaluateAchievements(base.copy(bestPrecisionStreak = doneVal))
            assertTrue("Expected unlocked at $doneVal for $achievement", achievement in done)
        }
    }

    @Test
    fun familiaDominioMixto_desbloqueaPorSesionesMixtas() {
        val base = EngagementProgress()

        // 1. Doble sentido: 5 sesiones Nivel 2 Mixto
        val doubleUnder = EngagementEngine.evaluateAchievements(base.copy(recognizerMixedSessions = 4))
        assertFalse(PermanentAchievement.DoubleMeaning in doubleUnder)
        val doubleDone = EngagementEngine.evaluateAchievements(base.copy(recognizerMixedSessions = 5))
        assertTrue(PermanentAchievement.DoubleMeaning in doubleDone)

        // 2. Lectura bidireccional: 15 sesiones acumuladas entre Nivel 2 Mixto y Nivel 3 Mixto
        val biUnder = EngagementEngine.evaluateAchievements(
            base.copy(recognizerMixedSessions = 8, challengeMixedSessions = 6), // total = 14
        )
        assertFalse(PermanentAchievement.BidirectionalReading in biUnder)

        val biDone = EngagementEngine.evaluateAchievements(
            base.copy(recognizerMixedSessions = 10, challengeMixedSessions = 5), // total = 15
        )
        assertTrue(PermanentAchievement.BidirectionalReading in biDone)
    }

    @Test
    fun precisionStreak_seIncrementaEnActividadesElegiblesYSoloAlPrimerIntentoSinPistas() {
        val date = PracticeDate(2026, 8, 29)
        var progress = EngagementProgress()

        // 1. Sesión Nivel 2 Mixto (elegible) con 15 ejercicios perfectos al primer intento
        val exerciseResults15 = List(15) {
            PracticeExerciseResult(firstAttemptCorrect = true, hintUsed = false)
        }
        val eligibleSession = EngagementSession(
            kind = PracticeSessionKind.Level2,
            exercisesCompleted = 15,
            firstAttemptCorrect = 15,
            mode = PracticeMode.Mixed,
            exerciseResults = exerciseResults15,
            isPrecisionEligible = true,
        )

        val update1 = EngagementEngine.recordSession(progress, eligibleSession, date)
        progress = update1.progress

        assertEquals(15, progress.currentPrecisionStreak)
        assertEquals(15, progress.bestPrecisionStreak)
        assertTrue(PermanentAchievement.BrailleFocus in progress.unlockedAchievements)
        assertTrue(PermanentAchievement.BrailleRhythm in progress.unlockedAchievements)
        assertTrue(PermanentAchievement.BraillePrecision in progress.unlockedAchievements)
        assertFalse(PermanentAchievement.SustainedReading in progress.unlockedAchievements)

        // 2. Segunda sesión Nivel 3 Mixto consecutiva con 20 ejercicios perfectos
        val exerciseResults20Part2 = List(20) {
            PracticeExerciseResult(firstAttemptCorrect = true, hintUsed = false)
        }
        val eligibleSession2 = EngagementSession(
            kind = PracticeSessionKind.Level3,
            exercisesCompleted = 20,
            firstAttemptCorrect = 20,
            mode = PracticeMode.Mixed,
            exerciseResults = exerciseResults20Part2,
            isPrecisionEligible = true,
        )

        val update2 = EngagementEngine.recordSession(progress, eligibleSession2, date)
        progress = update2.progress

        assertEquals(35, progress.currentPrecisionStreak)
        assertEquals(35, progress.bestPrecisionStreak)
        assertTrue(PermanentAchievement.SustainedReading in progress.unlockedAchievements)

        // 3. Si en una actividad elegible se comete un error al inicio, la racha actual se corta a 0 y luego suma
        val sessionWithError = EngagementSession(
            kind = PracticeSessionKind.Level2,
            exercisesCompleted = 15,
            firstAttemptCorrect = 14,
            errors = 1,
            mode = PracticeMode.Mixed,
            exerciseResults = listOf(PracticeExerciseResult(firstAttemptCorrect = false, hintUsed = false)) +
                List(14) { PracticeExerciseResult(firstAttemptCorrect = true, hintUsed = false) },
            isPrecisionEligible = true,
        )
        val update3 = EngagementEngine.recordSession(progress, sessionWithError, date)
        progress = update3.progress

        assertEquals(14, progress.currentPrecisionStreak)
        assertEquals(35, progress.bestPrecisionStreak)

        // 4. Si se usa una pista, también se corta la racha de precisión
        val sessionWithHint = EngagementSession(
            kind = PracticeSessionKind.Level2,
            exercisesCompleted = 15,
            firstAttemptCorrect = 15,
            hintsUsed = 1,
            mode = PracticeMode.Mixed,
            exerciseResults = listOf(PracticeExerciseResult(firstAttemptCorrect = true, hintUsed = true)) +
                List(14) { PracticeExerciseResult(firstAttemptCorrect = true, hintUsed = false) },
            isPrecisionEligible = true,
        )
        val update4 = EngagementEngine.recordSession(progress, sessionWithHint, date)
        progress = update4.progress

        assertEquals(14, progress.currentPrecisionStreak)
        assertEquals(35, progress.bestPrecisionStreak)
    }

    @Test
    fun sesionNoElegible_noModificaNiReiniciaLaRachaDePrecision() {
        val date = PracticeDate(2026, 8, 29)
        val initialProgress = EngagementProgress(
            currentPrecisionStreak = 12,
            bestPrecisionStreak = 25,
        )

        // Nivel 1 no es elegible
        val level1Session = EngagementSession(
            kind = PracticeSessionKind.Level1,
            exercisesCompleted = 10,
            firstAttemptCorrect = 2,
            errors = 8,
            hintsUsed = 5,
            isPrecisionEligible = false,
        )
        val update = EngagementEngine.recordSession(initialProgress, level1Session, date)

        assertEquals(12, update.progress.currentPrecisionStreak)
        assertEquals(25, update.progress.bestPrecisionStreak)
    }

    @Test
    fun fechasDeDesbloqueo_seGuardanYPersistenParaCadaLogro() {
        val d1 = PracticeDate(2026, 8, 10)
        val d2 = PracticeDate(2026, 8, 11)

        val s1 = EngagementSession(
            kind = PracticeSessionKind.Level1,
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
        )
        val update1 = EngagementEngine.recordSession(EngagementProgress(), s1, d1)
        assertTrue(PermanentAchievement.FirstStep in update1.progress.unlockedAchievements)
        assertEquals(d1, update1.progress.achievementUnlockDates[PermanentAchievement.FirstStep])

        val s2 = EngagementSession(
            kind = PracticeSessionKind.Level1,
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
        )
        val update2 = EngagementEngine.recordSession(update1.progress, s2, d2)
        assertEquals(d1, update2.progress.achievementUnlockDates[PermanentAchievement.FirstStep])
    }
}
