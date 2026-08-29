package com.brailuxaprende.play

import com.brailuxaprende.braille.BrailleCharacter
import java.util.UUID
import kotlin.random.Random

data class OrderItem(
    val character: BrailleCharacter,
    val isSolved: Boolean = false,
    val solvedOrder: Int? = null,
)

data class OrderRound(
    val roundNumber: Int,
    val items: List<OrderItem>,
    val expectedOrder: List<BrailleCharacter>,
    val currentExpectedIndex: Int = 0,
    val errors: Int = 0,
    val lastFeedbackMessage: OrderFeedback = OrderFeedback.None,
) {
    val isCompleted: Boolean = currentExpectedIndex >= expectedOrder.size
    val currentExpectedCharacter: BrailleCharacter? =
        expectedOrder.getOrNull(currentExpectedIndex)
}

enum class OrderFeedback {
    None,
    Correct,
    Incorrect,
}

data class OrderGameState(
    val sessionId: String = UUID.randomUUID().toString(),
    val rounds: List<OrderRound>,
    val currentRoundIndex: Int = 0,
    val isCompleted: Boolean = false,
) {
    val totalRounds: Int = rounds.size
    val currentRound: OrderRound? = rounds.getOrNull(currentRoundIndex)

    val totalErrors: Int = rounds.sumOf { it.errors }

    fun onSelectCharacter(character: BrailleCharacter): OrderGameState {
        if (isCompleted || currentRound == null) return this
        if (currentRound.isCompleted) return this

        val item = currentRound.items.firstOrNull {
            it.character.printedCharacter == character.printedCharacter
        } ?: return this

        if (item.isSolved) return this

        val expected = currentRound.currentExpectedCharacter ?: return this

        return if (character.printedCharacter == expected.printedCharacter) {
            val nextExpectedIdx = currentRound.currentExpectedIndex + 1
            val solvedNum = nextExpectedIdx

            val updatedItems = currentRound.items.map {
                if (it.character.printedCharacter == character.printedCharacter) {
                    it.copy(isSolved = true, solvedOrder = solvedNum)
                } else {
                    it
                }
            }
            val roundDone = nextExpectedIdx >= currentRound.expectedOrder.size

            val updatedRound = currentRound.copy(
                items = updatedItems,
                currentExpectedIndex = nextExpectedIdx,
                lastFeedbackMessage = OrderFeedback.Correct,
            )

            val updatedRounds = rounds.mapIndexed { index, round ->
                if (index == currentRoundIndex) updatedRound else round
            }

            if (roundDone) {
                val nextRoundIdx = currentRoundIndex + 1
                if (nextRoundIdx >= rounds.size) {
                    copy(rounds = updatedRounds, isCompleted = true)
                } else {
                    copy(rounds = updatedRounds, currentRoundIndex = nextRoundIdx)
                }
            } else {
                copy(rounds = updatedRounds)
            }
        } else {
            val updatedRound = currentRound.copy(
                errors = currentRound.errors + 1,
                lastFeedbackMessage = OrderFeedback.Incorrect,
            )
            val updatedRounds = rounds.mapIndexed { index, round ->
                if (index == currentRoundIndex) updatedRound else round
            }
            copy(rounds = updatedRounds)
        }
    }

    companion object {
        const val ROUNDS_COUNT = 5
        const val ITEMS_PER_ROUND = 5

        fun create(
            repertoire: List<BrailleCharacter>,
            random: Random = Random.Default,
            sessionId: String = UUID.randomUUID().toString(),
        ): OrderGameState {
            require(repertoire.size >= 5) {
                "El repertorio debe tener al menos 5 caracteres para jugar a Orden."
            }

            val itemsCount = minOf(ITEMS_PER_ROUND, repertoire.size)

            val rounds = (1..ROUNDS_COUNT).map { roundNum ->
                val selected = repertoire.shuffled(random).take(itemsCount)
                val expectedOrder = selected.sortedWith(PlayRepertoireProvider.spanishBrailleCharacterComparator)
                val shuffledItems = selected.shuffled(random).map { OrderItem(character = it) }

                OrderRound(
                    roundNumber = roundNum,
                    items = shuffledItems,
                    expectedOrder = expectedOrder,
                )
            }

            return OrderGameState(
                sessionId = sessionId,
                rounds = rounds,
            )
        }
    }
}
