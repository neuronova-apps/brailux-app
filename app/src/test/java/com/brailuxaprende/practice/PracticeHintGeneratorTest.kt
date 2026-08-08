package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.braille.BrailleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeHintGeneratorTest {
    @Test
    fun characterToSignLevel1StartsWithPointCountAndDescribesThePattern() {
        val exercise = exercise(
            target = 'H',
            type = PracticeExerciseType.CharacterToSign,
            optionCharacters = listOf('A', 'B', 'C', 'H'),
        )

        val hints = PracticeHintGenerator.generate(PracticeLevel.BrailleExplorer, exercise)

        assertEquals(PracticeHint.ActivePointCount(3), hints[0])
        assertEquals(PracticeHint.ColumnDistribution(leftCount = 2, rightCount = 1), hints[1])
        assertEquals(PracticeHint.PointState(point = 1, isActive = true), hints[2])
    }

    @Test
    fun characterToSignLevel2KeepsSubtleStructuralHints() {
        val exercise = exercise(
            target = 'L',
            type = PracticeExerciseType.CharacterToSign,
            optionCharacters = listOf('A', 'B', 'C', 'D', 'E', 'L'),
        )

        val hints = PracticeHintGenerator.generate(PracticeLevel.BrailleRecognizer, exercise)

        assertEquals(3, hints.size)
        assertTrue(hints[0] is PracticeHint.ColumnDistribution)
        assertTrue(hints[1] is PracticeHint.RowState)
        assertTrue(hints[2] is PracticeHint.PointState)
        assertFalse(hints.first() is PracticeHint.ActivePointCount)
    }

    @Test
    fun signToCharacterLevel1UsesCharacterInformationInsteadOfVisiblePointCount() {
        val exercise = exercise(
            target = 'A',
            type = PracticeExerciseType.SignToCharacter,
            optionCharacters = listOf('A', 'B', 'C', 'D'),
        )

        val hints = PracticeHintGenerator.generate(PracticeLevel.BrailleExplorer, exercise)

        assertEquals(PracticeHint.CharacterCategory(isVowel = true), hints[0])
        assertTrue(PracticeHint.AlphabetRange(first = 'A', last = 'E') in hints)
        assertTrue(
            PracticeHint.AlphabetComparison(reference = 'C', targetComesAfter = false) in hints,
        )
        assertFalse(hints.any { it is PracticeHint.ActivePointCount })
    }

    @Test
    fun signToCharacterHintsUseTheTargetAndVisibleOptions() {
        val earlyOptions = exercise(
            target = 'H',
            type = PracticeExerciseType.SignToCharacter,
            optionCharacters = listOf('A', 'B', 'F', 'H'),
        )
        val lateOptions = exercise(
            target = 'H',
            type = PracticeExerciseType.SignToCharacter,
            optionCharacters = listOf('G', 'H', 'I', 'J'),
        )

        val earlyComparison = PracticeHintGenerator.generate(
            PracticeLevel.BrailleExplorer,
            earlyOptions,
        ).filterIsInstance<PracticeHint.AlphabetComparison>().single()
        val lateComparison = PracticeHintGenerator.generate(
            PracticeLevel.BrailleExplorer,
            lateOptions,
        ).filterIsInstance<PracticeHint.AlphabetComparison>().single()

        assertEquals(
            PracticeHint.AlphabetComparison(reference = 'B', targetComesAfter = true),
            earlyComparison,
        )
        assertEquals(
            PracticeHint.AlphabetComparison(reference = 'J', targetComesAfter = false),
            lateComparison,
        )
    }

    @Test
    fun signToCharacterLevel2PrioritizesTheHintThatReducesVisibleOptions() {
        val exercise = exercise(
            target = 'Ñ',
            type = PracticeExerciseType.SignToCharacter,
            optionCharacters = listOf('K', 'M', 'N', 'Ñ', 'P', 'R'),
        )

        val hints = PracticeHintGenerator.generate(PracticeLevel.BrailleRecognizer, exercise)

        assertTrue(hints[0] is PracticeHint.AlphabetComparison)
        assertTrue(PracticeHint.AlphabetRange(first = 'K', last = 'R') in hints)
        assertTrue(PracticeHint.CharacterCategory(isVowel = false) in hints)
        assertFalse(hints.first() is PracticeHint.ActivePointCount)
    }

    @Test
    fun level3AlwaysReturnsNoHintsForEitherExerciseType() {
        PracticeExerciseType.entries.forEach { type ->
            val exercise = exercise(
                target = 'A',
                type = type,
                optionCharacters = listOf('A', 'B', 'C', 'D', 'E', 'F'),
            )

            assertTrue(
                PracticeHintGenerator.generate(PracticeLevel.BrailleChallenge, exercise).isEmpty(),
            )
        }
    }

    private fun exercise(
        target: Char,
        type: PracticeExerciseType,
        optionCharacters: List<Char>,
    ): PracticeExercise = PracticeExercise(
        target = character(target),
        type = type,
        options = optionCharacters.map(::character),
    )

    private fun character(value: Char): BrailleCharacter =
        requireNotNull(BrailleRepository.findCharacter(value))
}
