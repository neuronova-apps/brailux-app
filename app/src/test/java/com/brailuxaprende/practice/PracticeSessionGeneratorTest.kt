package com.brailuxaprende.practice

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionGeneratorTest {
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
