package com.brailuxaprende.play

import com.brailuxaprende.braille.BrailleRepository
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryGameTest {

    @Test
    fun `when repertoire has 5 vowels Memory creates exactly 5 pairs and 10 cards without duplicates`() {
        val vowels = BrailleRepository.getVowels()
        val state = MemoryGameState.create(repertoire = vowels, random = Random(42))

        assertEquals(5, state.totalPairs)
        assertEquals(10, state.cards.size)

        val letterCards = state.cards.filter { it.type == MemoryCardType.Letter }
        val brailleCards = state.cards.filter { it.type == MemoryCardType.Braille }

        assertEquals(5, letterCards.size)
        assertEquals(5, brailleCards.size)

        val letterChars = letterCards.map { it.character.printedCharacter }.toSet()
        val brailleChars = brailleCards.map { it.character.printedCharacter }.toSet()

        assertEquals(setOf('A', 'E', 'I', 'O', 'U'), letterChars)
        assertEquals(setOf('A', 'E', 'I', 'O', 'U'), brailleChars)
        assertEquals(5, letterCards.distinctBy { it.character.printedCharacter }.size)
    }

    @Test
    fun `when repertoire has 6 or more characters Memory creates exactly 6 pairs and 12 cards without duplicates`() {
        val repertoire = BrailleRepository.getLevel1Characters() // 10 characters A..J
        val state = MemoryGameState.create(repertoire = repertoire, random = Random(123))

        assertEquals(6, state.totalPairs)
        assertEquals(12, state.cards.size)

        val letterCards = state.cards.filter { it.type == MemoryCardType.Letter }
        val brailleCards = state.cards.filter { it.type == MemoryCardType.Braille }

        assertEquals(6, letterCards.size)
        assertEquals(6, brailleCards.size)

        val uniqueChars = letterCards.map { it.character.printedCharacter }
        assertEquals(6, uniqueChars.distinct().size)

        val brailleUniqueChars = brailleCards.map { it.character.printedCharacter }
        assertEquals(uniqueChars.toSet(), brailleUniqueChars.toSet())
    }

    @Test
    fun `opening matching letter and Braille cards marks pair as matched`() {
        val vowels = BrailleRepository.getVowels()
        var state = MemoryGameState.create(repertoire = vowels, random = Random(1))

        val firstCard = state.cards.first { it.character.printedCharacter == 'A' && it.type == MemoryCardType.Letter }
        val matchingCard = state.cards.first { it.character.printedCharacter == 'A' && it.type == MemoryCardType.Braille }

        state = state.onCardClick(firstCard.id)
        assertTrue(state.cards.first { it.id == firstCard.id }.isRevealed)
        assertFalse(state.cards.first { it.id == firstCard.id }.isMatched)
        assertEquals(0, state.moves)

        state = state.onCardClick(matchingCard.id)
        assertTrue(state.cards.first { it.id == firstCard.id }.isMatched)
        assertTrue(state.cards.first { it.id == matchingCard.id }.isMatched)
        assertEquals(1, state.matchedPairs)
        assertEquals(1, state.moves)
        assertFalse(state.isProcessingMismatch)
    }

    @Test
    fun `opening non-matching cards triggers mismatch state and can be dismissed`() {
        val vowels = BrailleRepository.getVowels()
        var state = MemoryGameState.create(repertoire = vowels, random = Random(1))

        val firstCard = state.cards.first { it.character.printedCharacter == 'A' && it.type == MemoryCardType.Letter }
        val nonMatchingCard = state.cards.first { it.character.printedCharacter == 'E' && it.type == MemoryCardType.Braille }

        state = state.onCardClick(firstCard.id)
        state = state.onCardClick(nonMatchingCard.id)

        assertEquals(1, state.moves)
        assertEquals(0, state.matchedPairs)
        assertTrue(state.isProcessingMismatch)
        assertTrue(state.cards.first { it.id == firstCard.id }.isRevealed)
        assertTrue(state.cards.first { it.id == nonMatchingCard.id }.isRevealed)

        state = state.dismissMismatch()
        assertFalse(state.isProcessingMismatch)
        assertFalse(state.cards.first { it.id == firstCard.id }.isRevealed)
        assertFalse(state.cards.first { it.id == nonMatchingCard.id }.isRevealed)
    }

    @Test
    fun `matching all pairs completes the game`() {
        val vowels = BrailleRepository.getVowels()
        var state = MemoryGameState.create(repertoire = vowels, random = Random(1))

        assertFalse(state.isCompleted)

        val characters = state.cards.map { it.character.printedCharacter }.distinct()
        for (char in characters) {
            val letterCard = state.cards.first { it.character.printedCharacter == char && it.type == MemoryCardType.Letter }
            val brailleCard = state.cards.first { it.character.printedCharacter == char && it.type == MemoryCardType.Braille }

            state = state.onCardClick(letterCard.id)
            state = state.onCardClick(brailleCard.id)
        }

        assertTrue(state.isCompleted)
        assertEquals(5, state.matchedPairs)
        assertEquals(5, state.moves)
    }
}
