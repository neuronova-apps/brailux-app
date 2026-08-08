package com.brailuxaprende.practice

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionStateTest {
    @Test
    fun level2StartsWithoutPointNumbersAndWithThreeHints() {
        val state = newLevel2State()

        assertFalse(state.showPointNumbers)
        assertEquals(3, state.hintsRemaining)
    }

    @Test
    fun level2LimitsHintsToThreeAndDoesNotSpendTwiceOnSameExercise() {
        var state = newLevel2State()

        state = state.showHint().showHint()
        assertEquals(1, state.hintsUsed)

        repeat(2) {
            state = answerCorrectlyAndAdvance(state).showHint()
        }

        assertEquals(3, state.hintsUsed)
        assertEquals(0, state.hintsRemaining)
        state = answerCorrectlyAndAdvance(state)
        assertFalse(state.showHint().hintVisible)
        assertEquals(3, state.showHint().hintsUsed)
    }

    @Test
    fun level2ValidatesAnswersCalculatesAccuracyAndCompletesAfterFifteenExercises() {
        var state = newLevel2State()

        repeat(15) { index ->
            if (index >= 12) {
                state = state.selectAnswer(incorrectAnswer(state)).checkAnswer()
                assertEquals(PracticeValidationState.Incorrect, state.validation)
            }
            state = answerCorrectlyAndAdvance(state)
        }

        assertTrue(state.isCompleted)
        assertEquals(15, state.completedExercises)
        assertEquals(12, state.firstAttemptCorrect)
        assertEquals(3, state.errors)
        assertEquals(80, state.accuracyPercentage)
        assertEquals(15, state.summary().exercisesCompleted)
    }

    @Test
    fun validatesCorrectAnswer() {
        val initial = newState()

        val checked = initial
            .selectAnswer(initial.currentExercise.target.printedCharacter)
            .checkAnswer()

        assertEquals(PracticeValidationState.Correct, checked.validation)
    }

    @Test
    fun validatesIncorrectAnswerWithoutBlockingRetry() {
        val initial = newState()
        val incorrect = incorrectAnswer(initial)

        val checked = initial.selectAnswer(incorrect).checkAnswer()
        val retried = checked.selectAnswer(initial.currentExercise.target.printedCharacter)

        assertEquals(PracticeValidationState.Incorrect, checked.validation)
        assertEquals(PracticeValidationState.AwaitingAnswer, retried.validation)
    }

    @Test
    fun countsCorrectAnswerOnFirstAttempt() {
        val initial = newState()

        val checked = initial
            .selectAnswer(initial.currentExercise.target.printedCharacter)
            .checkAnswer()

        assertEquals(1, checked.firstAttemptCorrect)
    }

    @Test
    fun incrementsErrorsAndDoesNotCountRetryAsFirstAttempt() {
        val initial = newState()
        val afterError = initial.selectAnswer(incorrectAnswer(initial)).checkAnswer()
        val afterCorrectRetry = afterError
            .selectAnswer(initial.currentExercise.target.printedCharacter)
            .checkAnswer()

        assertEquals(1, afterCorrectRetry.errors)
        assertEquals(0, afterCorrectRetry.firstAttemptCorrect)
        assertEquals(PracticeValidationState.Correct, afterCorrectRetry.validation)
    }

    @Test
    fun calculatesAccuracyFromFirstAttemptAnswers() {
        var state = newState()

        repeat(10) { index ->
            if (index >= 8) {
                state = state.selectAnswer(incorrectAnswer(state)).checkAnswer()
            }
            state = state
                .selectAnswer(state.currentExercise.target.printedCharacter)
                .checkAnswer()
                .nextExercise()
        }

        assertEquals(80, state.accuracyPercentage)
        assertEquals(2, state.errors)
    }

    @Test
    fun completesOnlyAfterTenExercises() {
        var state = newState()

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
    fun pointNumberVisibilityIsSessionStateOnly() {
        val state = newState()

        assertTrue(state.showPointNumbers)
        assertFalse(state.togglePointNumbers().showPointNumbers)
    }

    private fun newState(): PracticeSessionState = PracticeSessionState(
        PracticeSessionGenerator.generate(Random(12)),
    )

    private fun newLevel2State(): PracticeSessionState = PracticeSessionState(
        PracticeSessionGenerator.generateLevel2(Random(14)),
    )

    private fun incorrectAnswer(state: PracticeSessionState): Char = state.currentExercise.options
        .first { it.printedCharacter != state.currentExercise.target.printedCharacter }
        .printedCharacter

    private fun answerCorrectlyAndAdvance(state: PracticeSessionState): PracticeSessionState = state
        .selectAnswer(state.currentExercise.target.printedCharacter)
        .checkAnswer()
        .nextExercise()
}
