package com.brailuxaprende.practice

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionStateTest {
    @Test
    fun level3DisablesPointNumbersAndHintsCompletely() {
        val state = newLevel3State()

        assertFalse(state.showPointNumbers)
        assertFalse(state.togglePointNumbers().showPointNumbers)
        assertEquals(0, state.hintsRemaining)
        assertTrue(state.availableHints.isEmpty())
        assertFalse(state.canShowHint)
        assertFalse(state.showHint().hintVisible)
        assertEquals(0, state.showHint().hintsUsed)
    }

    @Test
    fun level3ValidatesCorrectAndIncorrectAnswersWithoutBlockingRetry() {
        val initial = newLevel3State()
        val afterError = initial.selectAnswer(incorrectAnswer(initial)).checkAnswer()
        val afterRetry = afterError
            .selectAnswer(initial.currentExercise.target.printedCharacter)
            .checkAnswer()

        assertEquals(PracticeValidationState.Incorrect, afterError.validation)
        assertEquals(1, afterError.errors)
        assertEquals(PracticeValidationState.Correct, afterRetry.validation)
        assertEquals(0, afterRetry.firstAttemptCorrect)
    }

    @Test
    fun level3CountsFirstAttemptsCalculatesAccuracyAndCompletesAfterTwentyExercises() {
        var state = newLevel3State()

        repeat(20) { index ->
            if (index >= 16) {
                state = state.selectAnswer(incorrectAnswer(state)).checkAnswer()
            }
            state = answerCorrectlyAndAdvance(state)
        }

        assertTrue(state.isCompleted)
        assertEquals(20, state.completedExercises)
        assertEquals(16, state.firstAttemptCorrect)
        assertEquals(4, state.errors)
        assertEquals(80, state.accuracyPercentage)
        assertEquals(20, state.summary().exercisesCompleted)
    }

    @Test
    fun level2StartsWithoutPointNumbersAndWithThreeHints() {
        val state = newLevel2State()

        assertFalse(state.showPointNumbers)
        assertEquals(3, state.hintsRemaining)
    }

    @Test
    fun level2RevealsThreeProgressiveHintsAndThenReachesZero() {
        var state = newLevel2State()

        repeat(3) { revealedHints ->
            state = state.showHint()
            assertEquals(revealedHints + 1, state.visibleHints.size)
            assertEquals(2 - revealedHints, state.hintsRemaining)
        }

        assertEquals(3, state.hintsUsed)
        assertEquals(0, state.hintsRemaining)
        assertFalse(state.canShowHint)
        assertEquals(state, state.showHint())
    }

    @Test
    fun level2HintLimitIsGlobalAndVisibleHintsResetOnAdvance() {
        var state = newLevel2State().showHint().showHint()

        state = answerCorrectlyAndAdvance(state)

        assertTrue(state.visibleHints.isEmpty())
        assertEquals(1, state.hintsRemaining)
        state = state.showHint()
        assertEquals(1, state.visibleHints.size)
        assertEquals(0, state.hintsRemaining)
    }

    @Test
    fun level1RevealsAllHintsWithoutSpendingTheSessionCounter() {
        var state = newState()

        repeat(3) {
            state = state.showHint()
        }

        assertEquals(state.availableHints, state.visibleHints)
        assertEquals(3, state.visibleHints.size)
        assertEquals(0, state.hintsUsed)
        assertEquals(null, state.hintsRemaining)
        assertFalse(state.canShowHint)
    }

    @Test
    fun level1VisibleHintsResetWhenAdvancingToANewExercise() {
        val state = answerCorrectlyAndAdvance(newState().showHint().showHint())

        assertTrue(state.visibleHints.isEmpty())
        assertTrue(state.canShowHint)
        assertEquals(0, state.hintsUsed)
    }

    @Test
    fun correctAnswerBlocksHintsWithoutSpendingOne() {
        val checked = newLevel2State().let { state ->
            state.selectAnswer(state.currentExercise.target.printedCharacter).checkAnswer()
        }

        val afterHintRequest = checked.showHint()

        assertFalse(checked.canShowHint)
        assertEquals(checked, afterHintRequest)
        assertEquals(0, afterHintRequest.hintsUsed)
        assertTrue(afterHintRequest.visibleHints.isEmpty())
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
        PracticeSessionGenerator.generate(random = Random(12)),
    )

    private fun newLevel2State(): PracticeSessionState = PracticeSessionState(
        PracticeSessionGenerator.generateLevel2(random = Random(14)),
    )

    private fun newLevel3State(): PracticeSessionState = PracticeSessionState(
        PracticeSessionGenerator.generateLevel3(random = Random(15)),
    )

    private fun incorrectAnswer(state: PracticeSessionState): Char = state.currentExercise.options
        .first { it.printedCharacter != state.currentExercise.target.printedCharacter }
        .printedCharacter

    private fun answerCorrectlyAndAdvance(state: PracticeSessionState): PracticeSessionState = state
        .selectAnswer(state.currentExercise.target.printedCharacter)
        .checkAnswer()
        .nextExercise()
}
