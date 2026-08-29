package com.brailuxaprende.play

import com.brailuxaprende.braille.BrailleCharacter
import java.util.UUID
import kotlin.random.Random

enum class MemoryCardType {
    Letter,
    Braille,
}

data class MemoryCard(
    val id: Int,
    val character: BrailleCharacter,
    val type: MemoryCardType,
    val isRevealed: Boolean = false,
    val isMatched: Boolean = false,
)

data class MemoryGameState(
    val sessionId: String = UUID.randomUUID().toString(),
    val cards: List<MemoryCard>,
    val firstSelectedCardId: Int? = null,
    val secondSelectedCardId: Int? = null,
    val moves: Int = 0,
    val isProcessingMismatch: Boolean = false,
) {
    val totalPairs: Int = cards.size / 2
    val matchedPairs: Int = cards.count { it.isMatched } / 2
    val isCompleted: Boolean = cards.isNotEmpty() && cards.all { it.isMatched }

    fun onCardClick(cardId: Int): MemoryGameState {
        if (isCompleted || isProcessingMismatch) return this

        val targetCard = cards.firstOrNull { it.id == cardId } ?: return this
        if (targetCard.isMatched || targetCard.isRevealed) return this

        if (firstSelectedCardId == null) {
            val updatedCards = cards.map { card ->
                if (card.id == cardId) card.copy(isRevealed = true) else card
            }
            return copy(
                cards = updatedCards,
                firstSelectedCardId = cardId,
                secondSelectedCardId = null,
            )
        }

        if (secondSelectedCardId == null && cardId != firstSelectedCardId) {
            val firstCard = cards.first { it.id == firstSelectedCardId }
            val isMatch = firstCard.character.printedCharacter == targetCard.character.printedCharacter &&
                firstCard.type != targetCard.type

            val newMoves = moves + 1

            return if (isMatch) {
                val updatedCards = cards.map { card ->
                    when (card.id) {
                        firstSelectedCardId, cardId ->
                            card.copy(isRevealed = true, isMatched = true)
                        else -> card
                    }
                }
                copy(
                    cards = updatedCards,
                    firstSelectedCardId = null,
                    secondSelectedCardId = null,
                    moves = newMoves,
                    isProcessingMismatch = false,
                )
            } else {
                val updatedCards = cards.map { card ->
                    if (card.id == cardId) card.copy(isRevealed = true) else card
                }
                copy(
                    cards = updatedCards,
                    secondSelectedCardId = cardId,
                    moves = newMoves,
                    isProcessingMismatch = true,
                )
            }
        }

        return this
    }

    fun dismissMismatch(): MemoryGameState {
        if (!isProcessingMismatch) return this
        val updatedCards = cards.map { card ->
            if (card.id == firstSelectedCardId || card.id == secondSelectedCardId) {
                card.copy(isRevealed = false)
            } else {
                card
            }
        }
        return copy(
            cards = updatedCards,
            firstSelectedCardId = null,
            secondSelectedCardId = null,
            isProcessingMismatch = false,
        )
    }

    companion object {
        fun create(
            repertoire: List<BrailleCharacter>,
            random: Random = Random.Default,
            sessionId: String = UUID.randomUUID().toString(),
        ): MemoryGameState {
            require(repertoire.size >= 5) {
                "El repertorio debe tener al menos 5 caracteres para jugar a Memoria."
            }
            val pairCount = minOf(6, repertoire.size)
            val selectedChars = repertoire.shuffled(random).take(pairCount)

            val rawCards = mutableListOf<MemoryCard>()
            var idCounter = 0
            for (char in selectedChars) {
                rawCards.add(
                    MemoryCard(
                        id = idCounter++,
                        character = char,
                        type = MemoryCardType.Letter,
                    ),
                )
                rawCards.add(
                    MemoryCard(
                        id = idCounter++,
                        character = char,
                        type = MemoryCardType.Braille,
                    ),
                )
            }

            val shuffledCards = rawCards.shuffled(random)

            return MemoryGameState(
                sessionId = sessionId,
                cards = shuffledCards,
            )
        }
    }
}
