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

    // -------------------------------------------------------------------------
    // NUEVAS PRUEBAS — Familia Constancia
    // -------------------------------------------------------------------------

    @Test
    fun familiaConstancia_rachasMuyPorEncimaDelRequisitoContinuanDesbloqueando() {
        val base = EngagementProgress()

        // bestStreak muy superior al umbral: todos los logros de racha deben estar desbloqueados
        val superProgress = EngagementEngine.evaluateAchievements(base.copy(bestStreak = 120))
        assertTrue(PermanentAchievement.ConstantWeek in superProgress)
        assertTrue(PermanentAchievement.TwoWeeks in superProgress)
        assertTrue(PermanentAchievement.ConsistencyMonth in superProgress)
        assertTrue(PermanentAchievement.SuperiorConsistency in superProgress)
    }

    @Test
    fun familiaConstancia_rachaActualEquivalenteABestDesbloqueaLogrosSiPreviaRachaEraInferior() {
        val base = EngagementProgress()

        // currentStreak alcanza el umbral aunque bestStreak sea 0 (estado inicial de un usuario nuevo
        // que nunca ha perdido la racha)
        val viaCurrent = EngagementEngine.evaluateAchievements(
            base.copy(currentStreak = 7, bestStreak = 0),
        )
        assertTrue(
            "Una racha actual de 7 debe desbloquear Semana constante aunque bestStreak sea 0",
            PermanentAchievement.ConstantWeek in viaCurrent,
        )
    }

    @Test
    fun familiaConstancia_usuarioConProgresoPrevioConservaLogrosYaDesbloqueados() {
        // Simula un usuario que ya tenía Constancia desbloqueada y practica un día más
        val date = PracticeDate(2026, 8, 10)
        val preExistingProgress = EngagementProgress(
            activityDates = setOf(
                PracticeDate(2026, 8, 7),
                PracticeDate(2026, 8, 8),
                PracticeDate(2026, 8, 9),
            ),
            bestStreak = 3,
            currentStreak = 3,
            lastActivityDate = PracticeDate(2026, 8, 9),
            unlockedAchievements = setOf(PermanentAchievement.Consistency),
        )
        val newSession = EngagementSession(
            kind = PracticeSessionKind.Level1,
            exercisesCompleted = 10,
            firstAttemptCorrect = 5,
        )
        val update = EngagementEngine.recordSession(preExistingProgress, newSession, date)

        assertTrue(
            "El logro Constancia previo debe conservarse",
            PermanentAchievement.Consistency in update.progress.unlockedAchievements,
        )
        assertFalse(
            "Constancia no debe aparecer como nuevo logro porque ya estaba desbloqueado",
            PermanentAchievement.Consistency in update.reward.newlyUnlockedAchievements,
        )
        assertEquals(4, update.progress.currentStreak)
        assertEquals(4, update.progress.bestStreak)
    }

    @Test
    fun familiaConstancia_semanaEnMovimiento_requiereExactamente5DiasEnLaMismaSemana() {
        val base = EngagementProgress()
        val lunes = PracticeDate(2026, 8, 3) // lunes

        // 4 días en la misma semana: no desbloquea
        val cuatroDias = (0..3).map { lunes.plusDays(it) }.toSet()
        val under4 = EngagementEngine.evaluateAchievements(base.copy(activityDates = cuatroDias))
        assertFalse(PermanentAchievement.WeekInMotion in under4)

        // 5 días en la misma semana (lunes–viernes): desbloquea
        val cincoDias = (0..4).map { lunes.plusDays(it) }.toSet()
        val done5 = EngagementEngine.evaluateAchievements(base.copy(activityDates = cincoDias))
        assertTrue(PermanentAchievement.WeekInMotion in done5)

        // 6 días en la misma semana: sigue desbloqueado (por encima del umbral)
        val seisDias = (0..5).map { lunes.plusDays(it) }.toSet()
        val over6 = EngagementEngine.evaluateAchievements(base.copy(activityDates = seisDias))
        assertTrue(PermanentAchievement.WeekInMotion in over6)
    }

    // -------------------------------------------------------------------------
    // PRUEBAS EXISTENTES — Familia Trayectoria Braille
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // NUEVAS PRUEBAS — Familia Trayectoria Braille
    // -------------------------------------------------------------------------

    @Test
    fun familiaTrayectoriaBraille_alcanzarUmbralSuperiorDesbloqueaInferioresImplicitamente() {
        val base = EngagementProgress()

        // Con 1200 ejercicios, TODOS los logros de la familia deben estar desbloqueados
        val supremacy = EngagementEngine.evaluateAchievements(base.copy(totalExercises = 1200))
        assertTrue(PermanentAchievement.Bronze in supremacy)
        assertTrue(PermanentAchievement.Silver in supremacy)
        assertTrue(PermanentAchievement.Gold in supremacy)
        assertTrue(PermanentAchievement.Platinum in supremacy)
        assertTrue(PermanentAchievement.Diamond in supremacy)
        assertTrue(PermanentAchievement.BrailleSupremacy in supremacy)
    }

    @Test
    fun familiaTrayectoriaBraille_progresionAcumulativa_cadaMedallaDesbloqueaSucesivamente() {
        val base = EngagementProgress()
        val date = PracticeDate(2026, 8, 10)

        // Level3 da 20 ejercicios por sesión; 2 sesiones = 40 ejercicios -> Bronze (25)
        val s1 = EngagementSession(
            kind = PracticeSessionKind.Level3,
            exercisesCompleted = 20,
            firstAttemptCorrect = 10,
        )
        var progress = EngagementEngine.recordSession(base, s1, date).progress
        progress = EngagementEngine.recordSession(progress, s1.copy(id = "s2"), date.plusDays(1)).progress

        assertEquals(40L, progress.totalExercises)
        assertTrue("Bronze debe estar desbloqueado con 40 ejercicios", PermanentAchievement.Bronze in progress.unlockedAchievements)
        assertFalse("Silver no debe desbloquearse aún con 40 ejercicios", PermanentAchievement.Silver in progress.unlockedAchievements)
    }

    @Test
    fun familiaTrayectoriaBraille_muyPorEncimaDelUmbral_logroSeConserva() {
        val base = EngagementProgress()

        // Ejercicios muy superiores a Supremacía (1200)
        val over = EngagementEngine.evaluateAchievements(base.copy(totalExercises = 5000))
        assertTrue(PermanentAchievement.BrailleSupremacy in over)
        assertTrue(PermanentAchievement.Diamond in over)
        assertTrue(PermanentAchievement.Platinum in over)
    }

    @Test
    fun familiaTrayectoriaBraille_usuarioConProgresoAcumuladoPrevioRecibeLogros() {
        // Simula usuario que ya tenía ejercicios almacenados antes de la evaluación
        val preExistingProgress = EngagementProgress(
            totalExercises = 74L,
            unlockedAchievements = setOf(PermanentAchievement.Bronze),
        )
        val date = PracticeDate(2026, 8, 10)
        val session = EngagementSession(
            kind = PracticeSessionKind.Level1,
            exercisesCompleted = 10,
            firstAttemptCorrect = 5,
        )
        val update = EngagementEngine.recordSession(preExistingProgress, session, date)

        // Con 84 ejercicios totales, Silver (75) debe desbloquearse en esta sesión
        assertEquals(84L, update.progress.totalExercises)
        assertTrue(PermanentAchievement.Silver in update.progress.unlockedAchievements)
        assertTrue(PermanentAchievement.Silver in update.reward.newlyUnlockedAchievements)
        // Bronze ya estaba desbloqueado, no debe aparecer como nuevo
        assertFalse(PermanentAchievement.Bronze in update.reward.newlyUnlockedAchievements)
    }

    // -------------------------------------------------------------------------
    // PRUEBAS EXISTENTES — Familia Precisión
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // NUEVAS PRUEBAS — Familia Precisión
    // -------------------------------------------------------------------------

    @Test
    fun familiaPrecision_alcanzarUmbralSuperiorDesbloqueaInferioresImplicitamente() {
        val base = EngagementProgress()

        // bestPrecisionStreak = 75 -> todos los logros de Precisión deben estar desbloqueados
        val all = EngagementEngine.evaluateAchievements(base.copy(bestPrecisionStreak = 75))
        assertTrue(PermanentAchievement.BrailleFocus in all)
        assertTrue(PermanentAchievement.BrailleRhythm in all)
        assertTrue(PermanentAchievement.BraillePrecision in all)
        assertTrue(PermanentAchievement.SustainedReading in all)
        assertTrue(PermanentAchievement.ConstantMastery in all)
        assertTrue(PermanentAchievement.SuperiorPrecision in all)
    }

    @Test
    fun familiaPrecision_muyPorEncimaDelUmbral_logroSeConserva() {
        val base = EngagementProgress()
        val wayOver = EngagementEngine.evaluateAchievements(base.copy(bestPrecisionStreak = 200))
        assertTrue(PermanentAchievement.SuperiorPrecision in wayOver)
        assertTrue(PermanentAchievement.ConstantMastery in wayOver)
    }

    @Test
    fun familiaPrecision_sincrRetroactivaViaEvaluate_usuarioConRachaYaAcumulada() {
        // Simula usuario con bestPrecisionStreak acumulado previamente (schema v2)
        // que no tenía los logros persistidos. evaluateAchievements debe desbloquearlo.
        val progressWithStreak = EngagementProgress(
            bestPrecisionStreak = 30,
            unlockedAchievements = emptySet(), // logros aún no evaluados
        )
        val evaluated = EngagementEngine.evaluateAchievements(progressWithStreak)

        assertTrue(
            "evaluateAchievements debe desbloquear BrailleFocus retroactivamente si bestPrecisionStreak >= 5",
            PermanentAchievement.BrailleFocus in evaluated,
        )
        assertTrue(
            "evaluateAchievements debe desbloquear BrailleRhythm retroactivamente si bestPrecisionStreak >= 10",
            PermanentAchievement.BrailleRhythm in evaluated,
        )
        assertTrue(
            "evaluateAchievements debe desbloquear BraillePrecision retroactivamente si bestPrecisionStreak >= 15",
            PermanentAchievement.BraillePrecision in evaluated,
        )
        assertTrue(
            "evaluateAchievements debe desbloquear SustainedReading retroactivamente si bestPrecisionStreak >= 30",
            PermanentAchievement.SustainedReading in evaluated,
        )
        assertFalse(
            "ConstantMastery no debe desbloquearse si bestPrecisionStreak < 50",
            PermanentAchievement.ConstantMastery in evaluated,
        )
    }

    @Test
    fun familiaPrecision_sesionElegibleIncrementaRachaYDesbloquea_sesionNoElegibleNoAltera() {
        val date = PracticeDate(2026, 8, 20)
        val base = EngagementProgress(bestPrecisionStreak = 4) // 1 por debajo de BrailleFocus

        // Sesión elegible: Level2 Mixto con 5 respuestas correctas consecutivas
        val eligibleSession = EngagementSession(
            kind = PracticeSessionKind.Level2,
            exercisesCompleted = 15,
            firstAttemptCorrect = 15,
            mode = PracticeMode.Mixed,
            exerciseResults = List(15) { PracticeExerciseResult(firstAttemptCorrect = true, hintUsed = false) },
            isPrecisionEligible = true,
        )
        val afterEligible = EngagementEngine.recordSession(base, eligibleSession, date)

        assertTrue(
            "BrailleFocus debe desbloquearse al alcanzar bestPrecisionStreak >= 5",
            PermanentAchievement.BrailleFocus in afterEligible.progress.unlockedAchievements,
        )

        // Sesión no elegible: no debe reducir la racha ni quitar el logro
        val nonEligibleSession = EngagementSession(
            kind = PracticeSessionKind.Level1,
            exercisesCompleted = 10,
            firstAttemptCorrect = 0,
            errors = 10,
            isPrecisionEligible = false,
        )
        val afterNonEligible = EngagementEngine.recordSession(
            afterEligible.progress,
            nonEligibleSession,
            date.plusDays(1),
        )

        assertTrue(
            "BrailleFocus debe conservarse después de una sesión no elegible",
            PermanentAchievement.BrailleFocus in afterNonEligible.progress.unlockedAchievements,
        )
        // bestPrecisionStreak no debe haber cambiado
        assertEquals(afterEligible.progress.bestPrecisionStreak, afterNonEligible.progress.bestPrecisionStreak)
    }

    @Test
    fun familiaPrecision_rachaSeCorta_mejorRachaSeConserva_logrosYaGanadosNoPierden() {
        val date = PracticeDate(2026, 8, 20)
        val base = EngagementProgress()

        // Primera sesión: alcanza racha 15
        val session15 = EngagementSession(
            kind = PracticeSessionKind.Level2,
            exercisesCompleted = 15,
            firstAttemptCorrect = 15,
            mode = PracticeMode.Mixed,
            exerciseResults = List(15) { PracticeExerciseResult(firstAttemptCorrect = true, hintUsed = false) },
            isPrecisionEligible = true,
        )
        val after15 = EngagementEngine.recordSession(base, session15, date)
        assertTrue(PermanentAchievement.BrailleRhythm in after15.progress.unlockedAchievements)
        assertEquals(15, after15.progress.bestPrecisionStreak)

        // Segunda sesión: error al inicio corta la racha actual, pero bestStreak se preserva
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
        val afterError = EngagementEngine.recordSession(after15.progress, sessionWithError, date.plusDays(1))

        assertEquals(14, afterError.progress.currentPrecisionStreak)
        assertEquals(15, afterError.progress.bestPrecisionStreak) // bestStreak no retrocede
        // Logro ya desbloqueado no desaparece
        assertTrue(
            "BrailleRhythm no debe perderse aunque la racha actual caiga a 14",
            PermanentAchievement.BrailleRhythm in afterError.progress.unlockedAchievements,
        )
    }

    @Test
    fun familiaPrecision_sincronizacionRepetidaEsIdempotente() {
        val base = EngagementProgress(bestPrecisionStreak = 50)

        val firstEval = EngagementEngine.evaluateAchievements(base)
        assertTrue(PermanentAchievement.ConstantMastery in firstEval)

        // Segunda evaluación sobre el mismo estado
        val secondEval = EngagementEngine.evaluateAchievements(base.copy(unlockedAchievements = firstEval))
        assertEquals(firstEval, secondEval)
    }

    // -------------------------------------------------------------------------
    // PRUEBAS EXISTENTES — Familia Dominio Mixto
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // NUEVAS PRUEBAS — Familia Dominio Mixto
    // -------------------------------------------------------------------------

    @Test
    fun familiaDominioMixto_doubleSentido_muyPorEncimaDelUmbral_persisteDesbloqueado() {
        val base = EngagementProgress()
        val over = EngagementEngine.evaluateAchievements(base.copy(recognizerMixedSessions = 20))
        assertTrue(PermanentAchievement.DoubleMeaning in over)
    }

    @Test
    fun familiaDominioMixto_lecturaBidireccional_soloConNivel2_cumpleRequisito() {
        val base = EngagementProgress()
        // Solo Nivel 2 Mixto, 15 sesiones: debe desbloquear ambos (Doble sentido Y Bidireccional)
        val onlyLevel2 = EngagementEngine.evaluateAchievements(
            base.copy(recognizerMixedSessions = 15, challengeMixedSessions = 0),
        )
        assertTrue(PermanentAchievement.DoubleMeaning in onlyLevel2)
        assertTrue(
            "15 sesiones solo en Nivel 2 Mixto también cumplen el requisito de Lectura bidireccional (acumuladas)",
            PermanentAchievement.BidirectionalReading in onlyLevel2,
        )
    }

    @Test
    fun familiaDominioMixto_acumulacionEnMomentosDistintos_desbloqueoCorrect() {
        val date1 = PracticeDate(2026, 8, 1)
        val date2 = PracticeDate(2026, 8, 15)
        var progress = EngagementProgress()

        // 5 sesiones Level2 Mixto en date1
        repeat(5) { i ->
            val s = EngagementSession(
                id = "l2-$i",
                kind = PracticeSessionKind.Level2,
                exercisesCompleted = 15,
                firstAttemptCorrect = 10,
                mode = PracticeMode.Mixed,
            )
            progress = EngagementEngine.recordSession(progress, s, date1).progress
        }
        assertTrue(
            "Doble sentido debe desbloquearse tras 5 sesiones Nivel 2 Mixto",
            PermanentAchievement.DoubleMeaning in progress.unlockedAchievements,
        )
        assertFalse(
            "Lectura bidireccional aún no debe desbloquearse con solo 5 sesiones",
            PermanentAchievement.BidirectionalReading in progress.unlockedAchievements,
        )

        // 10 sesiones Level3 Mixto en date2 (total acumulado: 15)
        repeat(10) { i ->
            val s = EngagementSession(
                id = "l3-$i",
                kind = PracticeSessionKind.Level3,
                exercisesCompleted = 20,
                firstAttemptCorrect = 15,
                mode = PracticeMode.Mixed,
            )
            progress = EngagementEngine.recordSession(progress, s, date2).progress
        }
        assertTrue(
            "Lectura bidireccional debe desbloquearse con 15 sesiones acumuladas (5 L2 + 10 L3)",
            PermanentAchievement.BidirectionalReading in progress.unlockedAchievements,
        )
    }

    @Test
    fun familiaDominioMixto_sesionNoMixta_noIncrementaContadorMixto() {
        val date = PracticeDate(2026, 8, 1)
        var progress = EngagementProgress()

        // Sesión Level2 en modo no-Mixto: no debe contar para recognizerMixedSessions
        val nonMixedSession = EngagementSession(
            kind = PracticeSessionKind.Level2,
            exercisesCompleted = 15,
            firstAttemptCorrect = 10,
            mode = PracticeMode.SignToCharacter,
        )
        progress = EngagementEngine.recordSession(progress, nonMixedSession, date).progress

        assertEquals(0, progress.recognizerMixedSessions)
        assertFalse(PermanentAchievement.DoubleMeaning in progress.unlockedAchievements)

        // Sesión Level2 Mixto: sí debe contar
        val mixedSession = EngagementSession(
            id = "mixed-1",
            kind = PracticeSessionKind.Level2,
            exercisesCompleted = 15,
            firstAttemptCorrect = 10,
            mode = PracticeMode.Mixed,
        )
        progress = EngagementEngine.recordSession(progress, mixedSession, date.plusDays(1)).progress

        assertEquals(1, progress.recognizerMixedSessions)
    }
    // -------------------------------------------------------------------------
    // NUEVAS PRUEBAS — Contador global y consistencia transversal
    // -------------------------------------------------------------------------

    @Test
    fun contadorGlobal_25LogrosActivos_coincideConLogrosRealesDesbloqueados() {
        val active = PermanentAchievement.activeEntries
        assertEquals("Deben existir exactamente 25 logros activos", 25, active.size)

        // Con progreso vacío: 0 desbloqueados
        val empty = EngagementProgress()
        val unlockedEmpty = active.count { it in empty.unlockedAchievements }
        assertEquals(0, unlockedEmpty)

        // Con progreso que cumple todos los requisitos: los 25 desbloqueados
        // Lunes 3 a Viernes 7 de Agosto 2026 (5 días en la misma semana para WeekInMotion)
        val fullLearn = LearningProgress(completedLessons = LearningLesson.entries.toSet())
        val fullProgress = EngagementProgress(
            totalSessions = 20,
            level1Sessions = 5,
            level2Sessions = 5,
            level3Sessions = 3,
            activityDates = (3..7).map { PracticeDate(2026, 8, it) }.toSet(),
            bestStreak = 60,
            currentStreak = 60,
            totalExercises = 1200,
            recognizerMixedSessions = 5,
            challengeMixedSessions = 10,
            bestPrecisionStreak = 75,
        )
        val allEvaluated = EngagementEngine.evaluateAchievements(fullProgress, fullLearn)
        val unlockedFull = active.count { it in allEvaluated }
        assertEquals(
            "Con todas las métricas cumplidas deben desbloquearse los 25 logros activos",
            25,
            unlockedFull,
        )
    }

    @Test
    fun contadorGlobal_logrosLegacyNoSeContabilizanEnLosActivos() {
        // HundredExercises desbloqueado no debe incrementar el contador de 25
        val progressWithLegacy = EngagementProgress(
            totalExercises = 100,
            unlockedAchievements = setOf(PermanentAchievement.HundredExercises),
        )
        val active = PermanentAchievement.activeEntries
        val unlockedActive = active.count { it in progressWithLegacy.unlockedAchievements }
        assertEquals(
            "HundredExercises (legacy) no debe aparecer en el contador de logros activos",
            0,
            unlockedActive,
        )
    }

    @Test
    fun fechaDeDesbloqueo_noSeModificaEnSincronizacionesRepetidas() {
        val originalDate = PracticeDate(2026, 8, 1)
        val laterDate = PracticeDate(2026, 8, 15)

        val session = EngagementSession(
            kind = PracticeSessionKind.Level1,
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
        )
        // Primera sesión: desbloquea FirstStep con fecha original
        val firstUpdate = EngagementEngine.recordSession(EngagementProgress(), session, originalDate)
        assertEquals(originalDate, firstUpdate.progress.achievementUnlockDates[PermanentAchievement.FirstStep])

        // Segunda sesión más tarde: la fecha de FirstStep no debe cambiar
        val secondSession = EngagementSession(
            id = "session-2",
            kind = PracticeSessionKind.Level1,
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
        )
        val secondUpdate = EngagementEngine.recordSession(firstUpdate.progress, secondSession, laterDate)
        assertEquals(
            "La fecha de desbloqueo original de FirstStep no debe modificarse",
            originalDate,
            secondUpdate.progress.achievementUnlockDates[PermanentAchievement.FirstStep],
        )
    }

    @Test
    fun ausenciaDeDuplicados_mismoLogroNoPuedeAparecerDosVecesEnUnlockedAchievements() {
        val date = PracticeDate(2026, 8, 10)
        var progress = EngagementProgress()

        // Varias sesiones que cumplirían el requisito de FirstStep múltiples veces
        repeat(5) { i ->
            val s = EngagementSession(
                id = "session-$i",
                kind = PracticeSessionKind.Level1,
                exercisesCompleted = 10,
                firstAttemptCorrect = 5,
            )
            progress = EngagementEngine.recordSession(progress, s, date.plusDays(i)).progress
        }

        val countFirstStep = progress.unlockedAchievements.count { it == PermanentAchievement.FirstStep }
        assertEquals(
            "FirstStep solo debe aparecer una vez en unlockedAchievements aunque se cumplan las condiciones múltiples veces",
            1,
            countFirstStep,
        )
    }
}

