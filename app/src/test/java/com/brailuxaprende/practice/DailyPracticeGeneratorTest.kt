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

    @Test
    fun dailyPracticeUsesLettersAToJWhenNoAdvancedLessonsAreCompleted() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDaily(
            date = date,
            learningProgress = com.brailuxaprende.data.learn.LearningProgress(),
            practiceProgress = com.brailuxaprende.data.practice.PracticeProgress(),
        )
        val validChars = ('A'..'J').toSet()

        assertEquals(5, session.exercises.size)
        assertTrue(session.exercises.all { it.target.printedCharacter in validChars })
        session.exercises.forEach { exercise ->
            assertTrue(exercise.options.all { it.printedCharacter in validChars })
        }
    }

    @Test
    fun dailyPracticeExpandsToLettersAToTWhenLessonKToTIsCompleted() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDaily(
            date = date,
            learningProgress = com.brailuxaprende.data.learn.LearningProgress(
                completedLessons = setOf(com.brailuxaprende.learning.LearningLesson.LettersKtoT),
            ),
            practiceProgress = com.brailuxaprende.data.practice.PracticeProgress(),
        )
        val validChars = ('A'..'T').toSet()

        assertEquals(5, session.exercises.size)
        assertTrue(session.exercises.all { it.target.printedCharacter in validChars })
    }

    @Test
    fun dailyPracticeUsesFullAlphabetWhenAdvancedLessonOrPracticeCompleted() {
        val date = PracticeDate(2026, 8, 28)
        val sessionFromLesson = PracticeSessionGenerator.generateDaily(
            date = date,
            learningProgress = com.brailuxaprende.data.learn.LearningProgress(
                completedLessons = setOf(com.brailuxaprende.learning.LearningLesson.LettersUtoZAndEnye),
            ),
        )
        val sessionFromPractice = PracticeSessionGenerator.generateDaily(
            date = date,
            practiceProgress = com.brailuxaprende.data.practice.PracticeProgress(
                level2CompletedSessions = 1,
            ),
        )
        val fullAlphabet = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toSet()

        assertTrue(sessionFromLesson.exercises.all { it.target.printedCharacter in fullAlphabet })
        assertTrue(sessionFromPractice.exercises.all { it.target.printedCharacter in fullAlphabet })
    }

    @Test
    fun dailyPracticeCombinesSignToLetterAndLetterToSignWithBalancedDistribution() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDaily(date = date)
        val signToLetterCount = session.exercises.count { it.type == PracticeExerciseType.SignToCharacter }
        val letterToSignCount = session.exercises.count { it.type == PracticeExerciseType.CharacterToSign }

        assertEquals(5, session.exercises.size)
        assertTrue((signToLetterCount == 3 && letterToSignCount == 2) || (signToLetterCount == 2 && letterToSignCount == 3))
    }

    private fun answerCorrectlyAndAdvance(state: PracticeSessionState): PracticeSessionState = state
        .selectAnswer(state.currentExercise.target.printedCharacter)
        .checkAnswer()
        .nextExercise()
}
