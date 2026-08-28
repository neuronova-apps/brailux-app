package com.brailuxaprende.practice

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionGeneratorTest {
    @Test
    fun level1SignToCharacterContainsOnlySignToCharacter() {
        val session = PracticeSessionGenerator.generate(PracticeMode.SignToCharacter, Random(101))
        assertEquals(PracticeMode.SignToCharacter, session.mode)
        assertEquals(10, session.exercises.size)
        assertTrue(session.exercises.all { it.type == PracticeExerciseType.SignToCharacter })
    }

    @Test
    fun level1CharacterToSignContainsOnlyCharacterToSign() {
        val session = PracticeSessionGenerator.generate(PracticeMode.CharacterToSign, Random(102))
        assertEquals(PracticeMode.CharacterToSign, session.mode)
        assertEquals(10, session.exercises.size)
        assertTrue(session.exercises.all { it.type == PracticeExerciseType.CharacterToSign })
    }

    @Test
    fun level1MixedContainsBothExerciseTypes() {
        val session = PracticeSessionGenerator.generate(PracticeMode.Mixed, Random(103))
        assertEquals(PracticeMode.Mixed, session.mode)
        assertEquals(10, session.exercises.size)
        assertEquals(PracticeExerciseType.entries.toSet(), session.exercises.map { it.type }.toSet())
    }

    @Test
    fun level2SignToCharacterContainsOnlySignToCharacter() {
        val session = PracticeSessionGenerator.generateLevel2(PracticeMode.SignToCharacter, Random(201))
        assertEquals(PracticeMode.SignToCharacter, session.mode)
        assertEquals(15, session.exercises.size)
        assertTrue(session.exercises.all { it.type == PracticeExerciseType.SignToCharacter })
    }

    @Test
    fun level2CharacterToSignContainsOnlyCharacterToSign() {
        val session = PracticeSessionGenerator.generateLevel2(PracticeMode.CharacterToSign, Random(202))
        assertEquals(PracticeMode.CharacterToSign, session.mode)
        assertEquals(15, session.exercises.size)
        assertTrue(session.exercises.all { it.type == PracticeExerciseType.CharacterToSign })
    }

    @Test
    fun level2MixedContainsBothExerciseTypes() {
        val session = PracticeSessionGenerator.generateLevel2(PracticeMode.Mixed, Random(203))
        assertEquals(PracticeMode.Mixed, session.mode)
        assertEquals(15, session.exercises.size)
        assertEquals(PracticeExerciseType.entries.toSet(), session.exercises.map { it.type }.toSet())
    }

    @Test
    fun level3SignToCharacterContainsOnlySignToCharacter() {
        val session = PracticeSessionGenerator.generateLevel3(PracticeMode.SignToCharacter, Random(301))
        assertEquals(PracticeMode.SignToCharacter, session.mode)
        assertEquals(20, session.exercises.size)
        assertTrue(session.exercises.all { it.type == PracticeExerciseType.SignToCharacter })
    }

    @Test
    fun level3CharacterToSignContainsOnlyCharacterToSign() {
        val session = PracticeSessionGenerator.generateLevel3(PracticeMode.CharacterToSign, Random(302))
        assertEquals(PracticeMode.CharacterToSign, session.mode)
        assertEquals(20, session.exercises.size)
        assertTrue(session.exercises.all { it.type == PracticeExerciseType.CharacterToSign })
    }

    @Test
    fun level3MixedContainsBothExerciseTypes() {
        val session = PracticeSessionGenerator.generateLevel3(PracticeMode.Mixed, Random(303))
        assertEquals(PracticeMode.Mixed, session.mode)
        assertEquals(20, session.exercises.size)
        assertEquals(PracticeExerciseType.entries.toSet(), session.exercises.map { it.type }.toSet())
    }

    @Test
    fun signToCharacterSessionContainsOnlyTheSelectedType() {
        val sessions = sessionsForAllLevels(PracticeMode.SignToCharacter)

        sessions.forEach { session ->
            assertEquals(PracticeMode.SignToCharacter, session.mode)
            assertTrue(session.exercises.all {
                it.type == PracticeExerciseType.SignToCharacter
            })
        }
    }

    @Test
    fun characterToSignSessionContainsOnlyTheSelectedType() {
        val sessions = sessionsForAllLevels(PracticeMode.CharacterToSign)

        sessions.forEach { session ->
            assertEquals(PracticeMode.CharacterToSign, session.mode)
            assertTrue(session.exercises.all {
                it.type == PracticeExerciseType.CharacterToSign
            })
        }
    }

    @Test
    fun mixedSessionContainsBothTypesWithBalancedAlternation() {
        val sessions = sessionsForAllLevels(PracticeMode.Mixed)

        sessions.forEach { session ->
            val types = session.exercises.map { it.type }
            assertEquals(PracticeMode.Mixed, session.mode)
            assertEquals(PracticeExerciseType.entries.toSet(), types.toSet())
            assertTrue(types.zipWithNext().all { (first, second) -> first != second })
            assertTrue(
                kotlin.math.abs(
                    types.count { it == PracticeExerciseType.SignToCharacter } -
                        types.count { it == PracticeExerciseType.CharacterToSign },
                ) <= 1,
            )
        }
    }

    @Test
    fun selectedModeRemainsFixedForTheWholeSession() {
        PracticeMode.entries.forEach { mode ->
            sessionsForAllLevels(mode).forEach { session ->
                val expectedTypes = when (mode) {
                    PracticeMode.SignToCharacter -> setOf(PracticeExerciseType.SignToCharacter)
                    PracticeMode.CharacterToSign -> setOf(PracticeExerciseType.CharacterToSign)
                    PracticeMode.Mixed -> PracticeExerciseType.entries.toSet()
                }
                assertEquals(expectedTypes, session.exercises.map { it.type }.toSet())
            }
        }
    }

    @Test
    fun levelsKeepTenFifteenAndTwentyExercises() {
        val sessions = sessionsForAllLevels(PracticeMode.Mixed)

        assertEquals(10, sessions[0].exercises.size)
        assertEquals(15, sessions[1].exercises.size)
        assertEquals(20, sessions[2].exercises.size)
    }

    @Test
    fun levelsKeepConfiguredDistinctOptionsIncludingTheAnswer() {
        sessionsForAllLevels(PracticeMode.Mixed).forEach { session ->
            session.exercises.forEach { exercise ->
                assertEquals(session.level.optionCount, exercise.options.size)
                assertEquals(
                    session.level.optionCount,
                    exercise.options.map { it.printedCharacter }.distinct().size,
                )
                assertTrue(exercise.target in exercise.options)
            }
        }
    }

    @Test
    fun level1IncludesEveryLetterFromAThroughJOnce() {
        val session = PracticeSessionGenerator.generate(
            mode = PracticeMode.SignToCharacter,
            random = Random(2),
        )

        assertEquals(
            ('A'..'J').toList(),
            session.exercises.map { it.target.printedCharacter }.sorted(),
        )
    }

    @Test
    fun levelsTwoAndThreeAvoidConsecutiveTargetRepetition() {
        val sessions = listOf(
            PracticeSessionGenerator.generateLevel2(PracticeMode.Mixed, Random(20)),
            PracticeSessionGenerator.generateLevel3(PracticeMode.Mixed, Random(30)),
        )

        sessions.forEach { session ->
            assertTrue(session.exercises.zipWithNext().all { (first, second) ->
                first.target.printedCharacter != second.target.printedCharacter
            })
        }
    }

    @Test
    fun levelsTwoAndThreeUseTheCompleteSpanishAlphabet() {
        val alphabet = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toSet()

        repeat(20) { seed ->
            val sessions = listOf(
                PracticeSessionGenerator.generateLevel2(PracticeMode.Mixed, Random(seed)),
                PracticeSessionGenerator.generateLevel3(PracticeMode.Mixed, Random(seed)),
            )
            sessions.forEach { session ->
                assertTrue(session.exercises.all { it.target.printedCharacter in alphabet })
            }
        }
    }

    private fun sessionsForAllLevels(mode: PracticeMode): List<PracticeSession> = listOf(
        PracticeSessionGenerator.generate(mode, Random(10)),
        PracticeSessionGenerator.generateLevel2(mode, Random(11)),
        PracticeSessionGenerator.generateLevel3(mode, Random(12)),
    )
}
