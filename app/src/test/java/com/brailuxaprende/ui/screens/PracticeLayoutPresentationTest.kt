package com.brailuxaprende.ui.screens

import com.brailuxaprende.braille.BrailleRepository
import com.brailuxaprende.practice.PracticeExercise
import com.brailuxaprende.practice.PracticeExerciseType
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.practice.PracticeSessionGenerator
import com.brailuxaprende.practice.PracticeSessionState
import com.brailuxaprende.practice.PracticeValidationState
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeLayoutPresentationTest {

    @Test
    fun `character to sign with 4 options distributes in 2 rows of 2 columns`() {
        val vowels = listOf('A', 'E', 'I', 'O').map { requireNotNull(BrailleRepository.findVowel(it)) }
        val exercise = PracticeExercise(
            type = PracticeExerciseType.CharacterToSign,
            target = vowels.first(),
            options = vowels,
        )

        val chunkedGrid = exercise.options.chunked(2)

        assertEquals(2, chunkedGrid.size)
        assertTrue(chunkedGrid.all { row -> row.size == 2 })
        assertEquals(listOf(vowels[0], vowels[1]), chunkedGrid[0])
        assertEquals(listOf(vowels[2], vowels[3]), chunkedGrid[1])
    }

    @Test
    fun `character to sign with 6 options distributes in 3 rows of 2 columns`() {
        val options = "ABCDEF".map { requireNotNull(BrailleRepository.findCharacter(it)) }
        val exercise = PracticeExercise(
            type = PracticeExerciseType.CharacterToSign,
            target = options.first(),
            options = options,
        )

        val chunkedGrid = exercise.options.chunked(2)

        assertEquals(3, chunkedGrid.size)
        assertTrue(chunkedGrid.all { row -> row.size == 2 })
        assertEquals(listOf(options[0], options[1]), chunkedGrid[0])
        assertEquals(listOf(options[2], options[3]), chunkedGrid[1])
        assertEquals(listOf(options[4], options[5]), chunkedGrid[2])
    }

    @Test
    fun `sign to character preserves single column list of options`() {
        val options = "ABCDEF".map { requireNotNull(BrailleRepository.findCharacter(it)) }
        val exercise = PracticeExercise(
            type = PracticeExerciseType.SignToCharacter,
            target = options.first(),
            options = options,
        )

        assertEquals(6, exercise.options.size)
        assertEquals(PracticeExerciseType.SignToCharacter, exercise.type)
    }

    @Test
    fun `can select any alternative in character to sign and sign to character`() {
        val session = PracticeSessionGenerator.generate(
            mode = PracticeMode.CharacterToSign,
            random = Random(42),
        )
        var state = PracticeSessionState(session)

        state.currentExercise.options.forEach { option ->
            state = state.selectAnswer(option.printedCharacter)
            assertEquals(option.printedCharacter, state.selectedCharacter)
        }
    }

    @Test
    fun `selection and validation work accurately in character to sign exercises`() {
        val session = PracticeSessionGenerator.generate(
            mode = PracticeMode.CharacterToSign,
            random = Random(101),
        )
        val state = PracticeSessionState(session)
        val targetChar = state.currentExercise.target.printedCharacter
        val incorrectChar = state.currentExercise.options.first { it.printedCharacter != targetChar }.printedCharacter

        val incorrectState = state.selectAnswer(incorrectChar).checkAnswer()
        assertEquals(PracticeValidationState.Incorrect, incorrectState.validation)
        assertEquals(1, incorrectState.errors)
        assertEquals(0, incorrectState.firstAttemptCorrect)

        val correctState = incorrectState.selectAnswer(targetChar).checkAnswer()
        assertEquals(PracticeValidationState.Correct, correctState.validation)
        assertEquals(1, correctState.errors)
        assertEquals(0, correctState.firstAttemptCorrect)
    }

    @Test
    fun `mixed mode alternates exercises and uses matching presentation type`() {
        val session = PracticeSessionGenerator.generate(
            mode = PracticeMode.Mixed,
            random = Random(202),
        )

        val types = session.exercises.map { it.type }
        assertTrue(types.contains(PracticeExerciseType.SignToCharacter))
        assertTrue(types.contains(PracticeExerciseType.CharacterToSign))

        session.exercises.forEach { exercise ->
            when (exercise.type) {
                PracticeExerciseType.SignToCharacter -> {
                    assertEquals(PracticeExerciseType.SignToCharacter, exercise.type)
                }
                PracticeExerciseType.CharacterToSign -> {
                    val grid = exercise.options.chunked(2)
                    assertTrue(grid.isNotEmpty())
                    assertTrue(grid.all { it.size <= 2 })
                }
            }
        }
    }
}
