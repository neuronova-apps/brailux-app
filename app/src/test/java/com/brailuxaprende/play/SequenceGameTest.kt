package com.brailuxaprende.play

import com.brailuxaprende.braille.BrailleRepository
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SequenceGameTest {

    @Test
    fun `Sequence creates exactly 5 rounds with progressive lengths 3, 3, 4, 4, 5`() {
        val repertoire = BrailleRepository.getLevel1Characters() // A..J
        val state = SequenceGameState.create(repertoire = repertoire, random = Random(42))

        assertEquals(5, state.totalRounds)
        assertEquals(listOf(3, 3, 4, 4, 5), state.rounds.map { it.length })
    }

    @Test
    fun `Sequence only uses characters from the provided repertoire`() {
        val repertoire = BrailleRepository.getLevel1Characters() // A..J
        val allowedChars = repertoire.map { it.printedCharacter }.toSet()
        val state = SequenceGameState.create(repertoire = repertoire, random = Random(99))

        for (round in state.rounds) {
            for (char in round.targetSequence) {
                assertTrue(char.printedCharacter in allowedChars)
            }
            for (opt in round.options) {
                assertTrue(opt.printedCharacter in allowedChars)
            }
        }
    }

    @Test
    fun `round progression through presentation, recall, feedback, and next round`() {
        val repertoire = BrailleRepository.getLevel1Characters()
        var state = SequenceGameState.create(repertoire = repertoire, random = Random(1))

        assertEquals(0, state.currentRoundIndex)
        assertEquals(SequenceRoundPhase.Presentation, state.currentRound?.phase)

        state = state.startRecall()
        assertEquals(SequenceRoundPhase.Recall, state.currentRound?.phase)

        val targetSeq = state.currentRound!!.targetSequence
        for (char in targetSeq) {
            state = state.onInputCharacter(char)
        }

        assertEquals(SequenceRoundPhase.Feedback, state.currentRound?.phase)
        assertTrue(state.currentRound?.isCorrect == true)

        state = state.onNextRound()
        assertEquals(1, state.currentRoundIndex)
        assertEquals(SequenceRoundPhase.Presentation, state.currentRound?.phase)
    }

    @Test
    fun `an incorrect sequence in a round records error but allows continuing the game`() {
        val repertoire = BrailleRepository.getLevel1Characters()
        var state = SequenceGameState.create(repertoire = repertoire, random = Random(1))

        state = state.startRecall()
        val targetSeq = state.currentRound!!.targetSequence

        // Input intentionally incorrect characters
        val wrongChar = repertoire.first { it.printedCharacter != targetSeq.first().printedCharacter }
        for (i in targetSeq.indices) {
            state = state.onInputCharacter(wrongChar)
        }

        assertEquals(SequenceRoundPhase.Feedback, state.currentRound?.phase)
        assertFalse(state.currentRound?.isCorrect == true)
        assertEquals(1, state.totalErrors)
        assertEquals(0, state.correctRoundsCount)

        // Advance to next round - game continues
        state = state.onNextRound()
        assertEquals(1, state.currentRoundIndex)
        assertFalse(state.isCompleted)
    }

    @Test
    fun `best length reflects the maximum length among correctly completed rounds`() {
        val repertoire = BrailleRepository.getLevel1Characters()
        var state = SequenceGameState.create(repertoire = repertoire, random = Random(1))

        // Play 5 rounds: Round 1 (len 3) correct, Round 2 (len 3) correct, Round 3 (len 4) incorrect, Round 4 (len 4) correct, Round 5 (len 5) incorrect
        for (i in 0..4) {
            state = state.startRecall()
            val targetSeq = state.currentRound!!.targetSequence
            if (i == 2 || i == 4) {
                // Incorrect
                val wrong = repertoire.first { it.printedCharacter != targetSeq.first().printedCharacter }
                repeat(targetSeq.size) { state = state.onInputCharacter(wrong) }
            } else {
                // Correct
                for (char in targetSeq) { state = state.onInputCharacter(char) }
            }
            state = state.onNextRound()
        }

        assertTrue(state.isCompleted)
        assertEquals(3, state.correctRoundsCount)
        assertEquals(2, state.totalErrors)
        assertEquals(4, state.bestLength) // Max length of correct rounds is 4 (round 4)
    }
}
