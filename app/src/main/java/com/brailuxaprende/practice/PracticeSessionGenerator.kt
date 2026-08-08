package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.braille.BrailleRepository
import kotlin.random.Random

object PracticeSessionGenerator {
    private const val OptionCount = 4

    fun generate(random: Random = Random.Default): PracticeSession {
        val characters = BrailleRepository.getLevel1Characters()
        require(characters.size == PracticeLevel.BrailleExplorer.exerciseCount) {
            "Braille Explorer requires the ten characters A through J."
        }

        val exercises = characters
            .shuffled(random)
            .mapIndexed { index, target ->
                PracticeExercise(
                    target = target,
                    type = if (index % 2 == 0) {
                        PracticeExerciseType.SignToCharacter
                    } else {
                        PracticeExerciseType.CharacterToSign
                    },
                    options = createOptions(target, characters, random),
                )
            }

        return PracticeSession(
            level = PracticeLevel.BrailleExplorer,
            exercises = exercises,
        )
    }

    private fun createOptions(
        target: BrailleCharacter,
        characters: List<BrailleCharacter>,
        random: Random,
    ): List<BrailleCharacter> = (characters
        .filterNot { it.printedCharacter == target.printedCharacter }
        .shuffled(random)
        .take(OptionCount - 1) + target)
        .shuffled(random)
}
