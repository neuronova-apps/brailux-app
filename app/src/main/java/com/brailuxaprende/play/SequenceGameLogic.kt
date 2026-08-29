package com.brailuxaprende.play

import com.brailuxaprende.braille.BrailleCharacter
import java.util.UUID
import kotlin.random.Random

enum class SequenceRoundPhase {
    Presentation,
    Recall,
    Feedback,
}

data class SequenceRound(
    val roundNumber: Int,
    val targetSequence: List<BrailleCharacter>,
    val options: List<BrailleCharacter>,
    val userSequence: List<BrailleCharacter> = emptyList(),
    val phase: SequenceRoundPhase = SequenceRoundPhase.Presentation,
    val isCorrect: Boolean? = null,
) {
    val length: Int = targetSequence.size
    val isFull: Boolean = userSequence.size == targetSequence.size
}

data class SequenceGameState(
    val sessionId: String = UUID.randomUUID().toString(),
    val rounds: List<SequenceRound>,
    val currentRoundIndex: Int = 0,
    val isCompleted: Boolean = false,
) {
    val totalRounds: Int = rounds.size
    val currentRound: SequenceRound? = rounds.getOrNull(currentRoundIndex)

    val correctRoundsCount: Int = rounds.count { it.isCorrect == true }
    val totalErrors: Int = rounds.count { it.isCorrect == false }
    val bestLength: Int = rounds
        .filter { it.isCorrect == true }
        .maxOfOrNull { it.length } ?: 0

    fun startRecall(): SequenceGameState {
        if (isCompleted || currentRound == null) return this
        if (currentRound.phase != SequenceRoundPhase.Presentation) return this

        val updatedRounds = rounds.mapIndexed { index, round ->
            if (index == currentRoundIndex) {
                round.copy(phase = SequenceRoundPhase.Recall)
            } else {
                round
            }
        }
        return copy(rounds = updatedRounds)
    }

    fun onInputCharacter(character: BrailleCharacter): SequenceGameState {
        if (isCompleted || currentRound == null) return this
        if (currentRound.phase != SequenceRoundPhase.Recall) return this
        if (currentRound.isFull) return this

        val newUserSeq = currentRound.userSequence + character
        val isNowFull = newUserSeq.size == currentRound.targetSequence.size

        if (isNowFull) {
            val isCorrect = newUserSeq.map { it.printedCharacter } ==
                currentRound.targetSequence.map { it.printedCharacter }
            val updatedRounds = rounds.mapIndexed { index, round ->
                if (index == currentRoundIndex) {
                    round.copy(
                        userSequence = newUserSeq,
                        phase = SequenceRoundPhase.Feedback,
                        isCorrect = isCorrect,
                    )
                } else {
                    round
                }
            }
            return copy(rounds = updatedRounds)
        } else {
            val updatedRounds = rounds.mapIndexed { index, round ->
                if (index == currentRoundIndex) {
                    round.copy(userSequence = newUserSeq)
                } else {
                    round
                }
            }
            return copy(rounds = updatedRounds)
        }
    }

    fun onRemoveLastInput(): SequenceGameState {
        if (isCompleted || currentRound == null) return this
        if (currentRound.phase != SequenceRoundPhase.Recall) return this
        if (currentRound.userSequence.isEmpty()) return this

        val newUserSeq = currentRound.userSequence.dropLast(1)
        val updatedRounds = rounds.mapIndexed { index, round ->
            if (index == currentRoundIndex) {
                round.copy(userSequence = newUserSeq)
            } else {
                round
            }
        }
        return copy(rounds = updatedRounds)
    }

    fun onNextRound(): SequenceGameState {
        if (isCompleted || currentRound == null) return this
        if (currentRound.phase != SequenceRoundPhase.Feedback) return this

        val nextIndex = currentRoundIndex + 1
        return if (nextIndex >= rounds.size) {
            copy(isCompleted = true)
        } else {
            copy(currentRoundIndex = nextIndex)
        }
    }

    companion object {
        val ROUND_LENGTHS = listOf(3, 3, 4, 4, 5)

        fun create(
            repertoire: List<BrailleCharacter>,
            random: Random = Random.Default,
            sessionId: String = UUID.randomUUID().toString(),
        ): SequenceGameState {
            require(repertoire.size >= 10) {
                "El repertorio debe tener al menos 10 caracteres (A-J) para jugar a Secuencia."
            }

            val rounds = ROUND_LENGTHS.mapIndexed { index, length ->
                // Choose sequence elements (can repeat or be random from repertoire)
                val targetSeq = (1..length).map {
                    repertoire.random(random)
                }
                // Options include distinct elements in sequence + distractors up to 6
                val targetDistinct = targetSeq.distinct()
                val remaining = repertoire.filter { it !in targetDistinct }.shuffled(random)
                val optionsPool = (targetDistinct + remaining).take(minOf(6, repertoire.size))
                val sortedOptions = optionsPool.sortedWith(PlayRepertoireProvider.spanishBrailleCharacterComparator)

                SequenceRound(
                    roundNumber = index + 1,
                    targetSequence = targetSeq,
                    options = sortedOptions,
                )
            }

            return SequenceGameState(
                sessionId = sessionId,
                rounds = rounds,
            )
        }
    }
}
