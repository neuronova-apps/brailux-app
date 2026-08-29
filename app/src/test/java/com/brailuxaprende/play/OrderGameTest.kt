package com.brailuxaprende.play

import com.brailuxaprende.braille.BrailleRepository
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderGameTest {

    @Test
    fun `Order creates exactly 5 rounds with unique characters per round`() {
        val repertoire = BrailleRepository.getLevel2Characters() // 27 characters
        val state = OrderGameState.create(repertoire = repertoire, random = Random(42))

        assertEquals(5, state.totalRounds)
        for (round in state.rounds) {
            assertEquals(5, round.items.size)
            val uniqueChars = round.items.map { it.character.printedCharacter }.distinct()
            assertEquals(5, uniqueChars.size)
        }
    }

    @Test
    fun `expected order uses Spanish alphabet order where Enye is strictly between N and O`() {
        val enye = BrailleRepository.findCharacter('Ñ')!!
        val n = BrailleRepository.findCharacter('N')!!
        val o = BrailleRepository.findCharacter('O')!!
        val a = BrailleRepository.findCharacter('A')!!
        val z = BrailleRepository.findCharacter('Z')!!

        val customRepertoire = listOf(o, z, a, enye, n)
        val state = OrderGameState.create(repertoire = customRepertoire, random = Random(1))

        val round = state.rounds.first()
        val expectedChars = round.expectedOrder.map { it.printedCharacter }
        assertEquals(listOf('A', 'N', 'Ñ', 'O', 'Z'), expectedChars)
    }

    @Test
    fun `tapping correct sign marks it solved and advances round`() {
        val repertoire = BrailleRepository.getLettersKToT() // 10 chars
        var state = OrderGameState.create(repertoire = repertoire, random = Random(1))

        val round = state.currentRound!!
        val firstExpected = round.expectedOrder.first()

        state = state.onSelectCharacter(firstExpected)

        val updatedFirst = state.currentRound!!.items.first { it.character.printedCharacter == firstExpected.printedCharacter }
        assertTrue(updatedFirst.isSolved)
        assertEquals(1, updatedFirst.solvedOrder)
        assertEquals(1, state.currentRound!!.currentExpectedIndex)
        assertEquals(0, state.totalErrors)
    }

    @Test
    fun `tapping wrong sign increments error and allows continuation without resetting round`() {
        val repertoire = BrailleRepository.getLettersKToT()
        var state = OrderGameState.create(repertoire = repertoire, random = Random(1))

        val round = state.currentRound!!
        val wrongChar = round.expectedOrder.last() // last instead of first

        state = state.onSelectCharacter(wrongChar)

        assertEquals(1, state.totalErrors)
        assertEquals(0, state.currentRound!!.currentExpectedIndex)
        val wrongItem = state.currentRound!!.items.first { it.character.printedCharacter == wrongChar.printedCharacter }
        assertFalse(wrongItem.isSolved)

        // Now tap the correct first
        val correctFirst = round.expectedOrder.first()
        state = state.onSelectCharacter(correctFirst)

        assertEquals(1, state.totalErrors)
        assertEquals(1, state.currentRound!!.currentExpectedIndex)
        assertTrue(state.currentRound!!.items.first { it.character.printedCharacter == correctFirst.printedCharacter }.isSolved)
    }

    @Test
    fun `completing all 5 rounds completes the game and aggregates errors`() {
        val repertoire = BrailleRepository.getLettersKToT()
        var state = OrderGameState.create(repertoire = repertoire, random = Random(1))

        for (r in 0..4) {
            val expectedList = state.currentRound!!.expectedOrder
            // Make one mistake on round 2 and 4
            if (r == 1 || r == 3) {
                state = state.onSelectCharacter(expectedList.last())
            }
            for (char in expectedList) {
                state = state.onSelectCharacter(char)
            }
        }

        assertTrue(state.isCompleted)
        assertEquals(2, state.totalErrors)
    }
}
