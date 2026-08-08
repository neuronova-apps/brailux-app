package com.brailuxaprende.practice

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPracticeTest {
    @Test
    fun spanishAlphabetIsAlwaysSelectedAndCannotBeRemoved() {
        val configuration = CustomPracticeConfiguration()
            .withContentGroup(PracticeContentGroup.SpanishAlphabet, selected = false)

        assertEquals(
            setOf(PracticeContentGroup.SpanishAlphabet),
            configuration.selectedContentGroups,
        )
    }

    @Test
    fun unavailableAdditionalGroupsCannotBeSelected() {
        PracticeContentGroup.entries
            .filterNot { it == PracticeContentGroup.SpanishAlphabet }
            .forEach { group ->
                val configuration = CustomPracticeConfiguration()
                    .withContentGroup(group, selected = true)

                assertFalse(group.isAvailable)
                assertTrue(configuration.additionalContentGroups.isEmpty())
            }
        assertTrue(
            CustomPracticeConfiguration().selectAllAvailableAdditional()
                .additionalContentGroups.isEmpty(),
        )
    }

    @Test
    fun generatesTenFifteenAndTwentyExercises() {
        CustomExerciseCount.entries.forEach { count ->
            val session = PracticeSessionGenerator.generateCustom(
                CustomPracticeConfiguration(exerciseCount = count),
                Random(count.value),
            )

            assertEquals(count.value, session.exercises.size)
        }
    }

    @Test
    fun signToCharacterModeUsesOnlySignToCharacterExercises() {
        val session = customSession(PracticeMode.SignToCharacter)

        assertTrue(session.exercises.all { it.type == PracticeExerciseType.SignToCharacter })
    }

    @Test
    fun characterToSignModeUsesOnlyCharacterToSignExercises() {
        val session = customSession(PracticeMode.CharacterToSign)

        assertTrue(session.exercises.all { it.type == PracticeExerciseType.CharacterToSign })
    }

    @Test
    fun mixedModeAlternatesBothExerciseTypes() {
        val session = customSession(PracticeMode.Mixed)
        val types = session.exercises.map { it.type }

        assertEquals(PracticeExerciseType.entries.toSet(), types.toSet())
        assertTrue(types.zipWithNext().all { (first, second) -> first != second })
    }

    @Test
    fun enabledHintsUseContextualGeneratorWithoutGlobalLimit() {
        val state = PracticeSessionState(
            PracticeSessionGenerator.generateCustom(
                CustomPracticeConfiguration(hintsEnabled = true),
                Random(20),
            ),
        )

        assertTrue(state.availableHints.isNotEmpty())
        assertEquals(null, state.hintsRemaining)
        assertEquals(1, state.showHint().hintsUsed)
    }

    @Test
    fun disabledHintsStayUnavailable() {
        val state = PracticeSessionState(
            PracticeSessionGenerator.generateCustom(
                CustomPracticeConfiguration(hintsEnabled = false),
                Random(21),
            ),
        )

        assertTrue(state.availableHints.isEmpty())
        assertFalse(state.canShowHint)
        assertEquals(0, state.showHint().hintsUsed)
    }

    @Test
    fun pointNumberVisibilityUsesConfigurationAndDoesNotChangeDuringSession() {
        val visible = customState(showPointNumbers = true)
        val hidden = customState(showPointNumbers = false)

        assertTrue(visible.showPointNumbers)
        assertFalse(hidden.showPointNumbers)
        assertEquals(visible, visible.togglePointNumbers())
        assertEquals(hidden, hidden.togglePointNumbers())
    }

    @Test
    fun sessionUsesOnlyCharactersFromSelectedContent() {
        val alphabet = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toSet()

        repeat(20) { seed ->
            val session = PracticeSessionGenerator.generateCustom(
                CustomPracticeConfiguration(exerciseCount = CustomExerciseCount.Twenty),
                Random(seed),
            )
            assertTrue(session.exercises.all { it.target.printedCharacter in alphabet })
            assertEquals(
                setOf(PracticeContentGroup.SpanishAlphabet),
                session.customConfiguration?.selectedContentGroups,
            )
        }
    }

    @Test
    fun generatedSessionHasNoConsecutiveDuplicatesOrDuplicateAlternatives() {
        repeat(30) { seed ->
            val session = PracticeSessionGenerator.generateCustom(
                CustomPracticeConfiguration(
                    exerciseCount = CustomExerciseCount.Twenty,
                    mode = PracticeMode.Mixed,
                ),
                Random(seed),
            )

            assertTrue(session.exercises.zipWithNext().all { (first, second) ->
                first.target.printedCharacter != second.target.printedCharacter && first != second
            })
            assertEquals(
                session.exercises.size,
                session.exercises.map { it.target.printedCharacter }.distinct().size,
            )
            assertTrue(session.exercises.all { exercise ->
                exercise.options.size == exercise.options.distinctBy { it.printedCharacter }.size
            })
        }
    }

    @Test
    fun calculatesAccuracyAndCompletesConfiguredSession() {
        var state = customState(showPointNumbers = true)

        repeat(10) { index ->
            if (index >= 8) {
                val incorrect = state.currentExercise.options.first {
                    it.printedCharacter != state.currentExercise.target.printedCharacter
                }.printedCharacter
                state = state.selectAnswer(incorrect).checkAnswer()
            }
            state = state
                .selectAnswer(state.currentExercise.target.printedCharacter)
                .checkAnswer()
                .nextExercise()
        }

        assertTrue(state.isCompleted)
        assertEquals(10, state.completedExercises)
        assertEquals(8, state.firstAttemptCorrect)
        assertEquals(2, state.errors)
        assertEquals(80, state.accuracyPercentage)
        assertEquals(10, state.summary().exercisesCompleted)
    }

    private fun customSession(mode: PracticeMode): PracticeSession =
        PracticeSessionGenerator.generateCustom(
            CustomPracticeConfiguration(mode = mode),
            Random(mode.ordinal + 10),
        )

    private fun customState(showPointNumbers: Boolean): PracticeSessionState =
        PracticeSessionState(
            PracticeSessionGenerator.generateCustom(
                CustomPracticeConfiguration(showPointNumbers = showPointNumbers),
                Random(if (showPointNumbers) 30 else 31),
            ),
        )
}
