package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.braille.BrailleRepository
import kotlin.random.Random

object PracticeSessionGenerator {
    fun generate(random: Random = Random.Default): PracticeSession {
        return generateLevel(PracticeLevel.BrailleExplorer, random)
    }

    fun generateLevel2(random: Random = Random.Default): PracticeSession {
        return generateLevel(PracticeLevel.BrailleRecognizer, random)
    }

    private fun generateLevel(level: PracticeLevel, random: Random): PracticeSession {
        val characters = when (level) {
            PracticeLevel.BrailleExplorer -> BrailleRepository.getLevel1Characters()
            PracticeLevel.BrailleRecognizer -> BrailleRepository.getLevel2Characters()
        }

        val targets = when (level) {
            PracticeLevel.BrailleExplorer -> characters.shuffled(random)
            PracticeLevel.BrailleRecognizer -> buildList {
                repeat(level.exerciseCount) {
                    val previous = lastOrNull()
                    add(characters.filterNot { it == previous }.random(random))
                }
            }
        }

        val exercises = targets.mapIndexed { index, target ->
            PracticeExercise(
                target = target,
                type = if (index % 2 == 0) {
                    PracticeExerciseType.SignToCharacter
                } else {
                    PracticeExerciseType.CharacterToSign
                },
                options = createOptions(target, characters, level.optionCount, random),
            )
        }

        return PracticeSession(level = level, exercises = exercises)
    }

    private fun createOptions(
        target: BrailleCharacter,
        characters: List<BrailleCharacter>,
        optionCount: Int,
        random: Random,
    ): List<BrailleCharacter> = (characters
        .filterNot { it.printedCharacter == target.printedCharacter }
        .shuffled(random)
        .take(optionCount - 1) + target)
        .shuffled(random)
}
