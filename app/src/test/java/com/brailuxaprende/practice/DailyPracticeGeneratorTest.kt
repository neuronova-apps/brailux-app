package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleRepository
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPracticeGeneratorTest {
    @Test
    fun dailySessionContainsExactlyFiveMixedExercises() {
        val session = PracticeSessionGenerator.generateDaily(Random(8))

        assertEquals(PracticeLevel.Daily, session.level)
        assertEquals(PracticeMode.Mixed, session.mode)
        assertEquals(5, session.exercises.size)
        assertEquals(PracticeExerciseType.entries.toSet(), session.exercises.map { it.type }.toSet())
    }

    @Test
    fun dailyTargetsAreVariedAndUseOnlyAvailableSpanishAlphabet() {
        val availableCharacters = BrailleRepository.getLevel2Characters()
            .map { it.printedCharacter }
            .toSet()

        repeat(30) { seed ->
            val session = PracticeSessionGenerator.generateDaily(Random(seed))
            val targets = session.exercises.map { it.target.printedCharacter }

            assertEquals(5, targets.distinct().size)
            assertTrue(targets.all { it in availableCharacters })
            assertTrue(targets.zipWithNext().all { (first, second) -> first != second })
        }
    }

    @Test
    fun everyExerciseHasFourDistinctOptionsIncludingItsAnswer() {
        repeat(30) { seed ->
            val session = PracticeSessionGenerator.generateDaily(Random(seed))

            session.exercises.forEach { exercise ->
                assertEquals(PracticeLevel.Daily.optionCount, exercise.options.size)
                assertEquals(
                    PracticeLevel.Daily.optionCount,
                    exercise.options.distinctBy { it.printedCharacter }.size,
                )
                assertTrue(exercise.target in exercise.options)
            }
        }
    }

    @Test
    fun fixedSeedProducesAReproducibleBalancedSession() {
        val first = PracticeSessionGenerator.generateDaily(Random(42))
        val second = PracticeSessionGenerator.generateDaily(Random(42))

        assertEquals(first, second)
        assertTrue(first.exercises.zipWithNext().all { (left, right) -> left.type != right.type })
    }

    @Test
    fun generatedDailySessionCompletesOnlyAfterItsFifthExercise() {
        var state = PracticeSessionState(PracticeSessionGenerator.generateDaily(Random(16)))

        repeat(4) {
            state = answerCorrectlyAndAdvance(state)
            assertFalse(state.isCompleted)
        }
        state = answerCorrectlyAndAdvance(state)

        assertTrue(state.isCompleted)
        assertEquals(5, state.completedExercises)
        assertEquals(5, state.summary().exercisesCompleted)
    }

    private fun answerCorrectlyAndAdvance(state: PracticeSessionState): PracticeSessionState = state
        .selectAnswer(state.currentExercise.target.printedCharacter)
        .checkAnswer()
        .nextExercise()
}
