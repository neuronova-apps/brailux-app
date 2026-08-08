package com.brailuxaprende.practice

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionGeneratorTest {
    @Test
    fun level3GeneratesTwentyExercisesWithoutConsecutiveRepetition() {
        val session = PracticeSessionGenerator.generateLevel3(Random(30))

        assertEquals(PracticeLevel.BrailleChallenge, session.level)
        assertEquals(20, session.exercises.size)
        assertTrue(
            session.exercises.zipWithNext().all { (first, second) ->
                first.target.printedCharacter != second.target.printedCharacter && first != second
            },
        )
    }

    @Test
    fun level3AlternatesTypesAndProvidesSixDistinctOptions() {
        val session = PracticeSessionGenerator.generateLevel3(Random(31))

        assertEquals(PracticeExerciseType.SignToCharacter, session.exercises.first().type)
        assertTrue(session.exercises.map { it.type }.zipWithNext().all { (first, second) ->
            first != second
        })
        session.exercises.forEach { exercise ->
            assertEquals(6, exercise.options.size)
            assertEquals(6, exercise.options.map { it.printedCharacter }.distinct().size)
            assertTrue(exercise.target in exercise.options)
        }
    }

    @Test
    fun level3UsesCompleteSpanishAlphabet() {
        val alphabet = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toSet()

        repeat(20) { seed ->
            val session = PracticeSessionGenerator.generateLevel3(Random(seed))
            assertTrue(session.exercises.all { it.target.printedCharacter in alphabet })
        }
    }

    @Test
    fun level2GeneratesFifteenExercisesWithoutConsecutiveRepetition() {
        val session = PracticeSessionGenerator.generateLevel2(Random(20))

        assertEquals(PracticeLevel.BrailleRecognizer, session.level)
        assertEquals(15, session.exercises.size)
        assertTrue(
            session.exercises.zipWithNext().all { (first, second) ->
                first.target.printedCharacter != second.target.printedCharacter && first != second
            },
        )
    }

    @Test
    fun level2AlternatesTypesAndProvidesMoreOptionsThanLevel1() {
        val level1 = PracticeSessionGenerator.generate(Random(21))
        val level2 = PracticeSessionGenerator.generateLevel2(Random(21))

        assertTrue(level2.exercises.map { it.type }.zipWithNext().all { (first, second) ->
            first != second
        })
        assertEquals(PracticeExerciseType.SignToCharacter, level2.exercises.first().type)
        assertTrue(level2.exercises.all { it.options.size == 6 })
        assertTrue(level2.exercises.first().options.size > level1.exercises.first().options.size)
    }

    @Test
    fun level2SelectsTargetsFromCompleteSpanishAlphabet() {
        val alphabet = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toSet()

        repeat(20) { seed ->
            val session = PracticeSessionGenerator.generateLevel2(Random(seed))
            assertTrue(session.exercises.all { it.target.printedCharacter in alphabet })
        }
    }

    @Test
    fun generatesTenExercises() {
        val session = PracticeSessionGenerator.generate(Random(1))

        assertEquals(10, session.exercises.size)
    }

    @Test
    fun includesEveryLetterFromAThroughJOnce() {
        val session = PracticeSessionGenerator.generate(Random(2))

        assertEquals(
            ('A'..'J').toList(),
            session.exercises.map { it.target.printedCharacter }.sorted(),
        )
    }

    @Test
    fun doesNotRepeatTheSameQuestionConsecutively() {
        val session = PracticeSessionGenerator.generate(Random(3))
        val targets = session.exercises.map { it.target.printedCharacter }

        assertTrue(targets.zipWithNext().all { (first, second) -> first != second })
    }

    @Test
    fun alternatesExerciseTypes() {
        val session = PracticeSessionGenerator.generate(Random(4))
        val types = session.exercises.map { it.type }

        assertTrue(types.zipWithNext().all { (first, second) -> first != second })
        assertEquals(PracticeExerciseType.SignToCharacter, types.first())
    }

    @Test
    fun eachExerciseHasFourDistinctOptionsIncludingTheAnswer() {
        val session = PracticeSessionGenerator.generate(Random(5))

        session.exercises.forEach { exercise ->
            assertEquals(4, exercise.options.size)
            assertEquals(4, exercise.options.map { it.printedCharacter }.distinct().size)
            assertTrue(exercise.target in exercise.options)
        }
    }
}
