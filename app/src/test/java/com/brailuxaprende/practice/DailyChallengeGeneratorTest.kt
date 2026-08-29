package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleRepository
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.learning.LearningLesson
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeGeneratorTest {
    @Test
    fun dailyChallengeSessionContainsExactlyTenMixedExercises() {
        val session = PracticeSessionGenerator.generateDailyChallenge(Random(8))

        assertEquals(PracticeLevel.DailyChallenge, session.level)
        assertEquals(PracticeMode.Mixed, session.mode)
        assertEquals(10, session.exercises.size)
        assertEquals(PracticeExerciseType.entries.toSet(), session.exercises.map { it.type }.toSet())
    }

    @Test
    fun dailyChallengeHasExactlyFiveSignToCharacterAndFiveCharacterToSign() {
        repeat(30) { seed ->
            val session = PracticeSessionGenerator.generateDailyChallenge(Random(seed))
            val signToLetterCount = session.exercises.count { it.type == PracticeExerciseType.SignToCharacter }
            val letterToSignCount = session.exercises.count { it.type == PracticeExerciseType.CharacterToSign }

            assertEquals(10, session.exercises.size)
            assertEquals(5, signToLetterCount)
            assertEquals(5, letterToSignCount)
        }
    }

    @Test
    fun dailyChallengeTargetsAreVariedAndUseAvailableSpanishAlphabet() {
        val availableCharacters = BrailleRepository.getLevel2Characters()
            .map { it.printedCharacter }
            .toSet()

        repeat(30) { seed ->
            val session = PracticeSessionGenerator.generateDailyChallenge(Random(seed))
            val targets = session.exercises.map { it.target.printedCharacter }

            assertEquals(10, targets.distinct().size)
            assertTrue(targets.all { it in availableCharacters })
            assertTrue(targets.zipWithNext().all { (first, second) -> first != second })
        }
    }

    @Test
    fun everyExerciseHasFourDistinctOptionsIncludingItsAnswer() {
        repeat(30) { seed ->
            val session = PracticeSessionGenerator.generateDailyChallenge(Random(seed))

            session.exercises.forEach { exercise ->
                assertEquals(PracticeLevel.DailyChallenge.optionCount, exercise.options.size)
                assertEquals(
                    PracticeLevel.DailyChallenge.optionCount,
                    exercise.options.distinctBy { it.printedCharacter }.size,
                )
                assertTrue(exercise.target in exercise.options)
            }
        }
    }

    @Test
    fun fixedSeedProducesAReproducibleBalancedSession() {
        val date = PracticeDate(2026, 8, 28)
        val first = PracticeSessionGenerator.generateDailyChallenge(date = date)
        val second = PracticeSessionGenerator.generateDailyChallenge(date = date)

        assertEquals(first, second)
        assertTrue(first.exercises.zipWithNext().all { (left, right) -> left.type != right.type })
    }

    @Test
    fun generatedDailyChallengeSessionCompletesOnlyAfterItsTenthExercise() {
        var state = PracticeSessionState(PracticeSessionGenerator.generateDailyChallenge(Random(16)))

        repeat(9) {
            state = answerCorrectlyAndAdvance(state)
            assertFalse(state.isCompleted)
        }
        state = answerCorrectlyAndAdvance(state)

        assertTrue(state.isCompleted)
        assertEquals(10, state.completedExercises)
        assertEquals(10, state.summary().exercisesCompleted)
    }

    @Test
    fun dailyChallengeUsesLettersAToJWhenNoAdvancedLessonsAreCompleted() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDailyChallenge(
            date = date,
            learningProgress = LearningProgress(),
            practiceProgress = PracticeProgress(),
        )
        val validChars = ('A'..'J').toSet()

        assertEquals(10, session.exercises.size)
        assertTrue(session.exercises.all { it.target.printedCharacter in validChars })
        session.exercises.forEach { exercise ->
            assertTrue(exercise.options.all { it.printedCharacter in validChars })
        }
    }

    @Test
    fun dailyChallengeExpandsToLettersAToTWhenLessonKToTIsCompleted() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDailyChallenge(
            date = date,
            learningProgress = LearningProgress(
                completedLessons = setOf(LearningLesson.LettersKtoT),
            ),
            practiceProgress = PracticeProgress(),
        )
        val validChars = ('A'..'T').toSet()

        assertEquals(10, session.exercises.size)
        assertTrue(session.exercises.all { it.target.printedCharacter in validChars })
        assertEquals(10, session.exercises.map { it.target.printedCharacter }.distinct().size)
    }

    @Test
    fun dailyChallengeUsesFullAlphabetWhenAdvancedLessonOrPracticeCompleted() {
        val date = PracticeDate(2026, 8, 28)
        val sessionFromLesson = PracticeSessionGenerator.generateDailyChallenge(
            date = date,
            learningProgress = LearningProgress(
                completedLessons = setOf(LearningLesson.LettersUtoZAndEnye),
            ),
        )
        val sessionFromPractice = PracticeSessionGenerator.generateDailyChallenge(
            date = date,
            practiceProgress = PracticeProgress(
                level2CompletedSessions = 1,
            ),
        )
        val fullAlphabet = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toSet()

        assertEquals(10, sessionFromLesson.exercises.size)
        assertEquals(10, sessionFromPractice.exercises.size)
        assertTrue(sessionFromLesson.exercises.all { it.target.printedCharacter in fullAlphabet })
        assertTrue(sessionFromPractice.exercises.all { it.target.printedCharacter in fullAlphabet })
    }

    private fun answerCorrectlyAndAdvance(state: PracticeSessionState): PracticeSessionState = state
        .selectAnswer(state.currentExercise.target.printedCharacter)
        .checkAnswer()
        .nextExercise()
}
