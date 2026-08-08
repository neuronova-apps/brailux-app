package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.braille.BrailleRepository
import kotlin.random.Random

object PracticeSessionGenerator {
    fun generate(
        mode: PracticeMode = PracticeMode.SignToCharacter,
        random: Random = Random.Default,
    ): PracticeSession {
        return generateLevel(PracticeLevel.BrailleExplorer, mode, random)
    }

    fun generateLevel2(
        mode: PracticeMode = PracticeMode.SignToCharacter,
        random: Random = Random.Default,
    ): PracticeSession {
        return generateLevel(PracticeLevel.BrailleRecognizer, mode, random)
    }

    fun generateLevel3(
        mode: PracticeMode = PracticeMode.SignToCharacter,
        random: Random = Random.Default,
    ): PracticeSession {
        return generateLevel(PracticeLevel.BrailleChallenge, mode, random)
    }

    private fun generateLevel(
        level: PracticeLevel,
        mode: PracticeMode,
        random: Random,
    ): PracticeSession {
        val characters = when (level) {
            PracticeLevel.BrailleExplorer -> BrailleRepository.getLevel1Characters()
            PracticeLevel.BrailleRecognizer -> BrailleRepository.getLevel2Characters()
            PracticeLevel.BrailleChallenge -> BrailleRepository.getLevel2Characters()
        }

        val targets = when (level) {
            PracticeLevel.BrailleExplorer -> characters.shuffled(random)
            PracticeLevel.BrailleRecognizer,
            PracticeLevel.BrailleChallenge -> buildList {
                repeat(level.exerciseCount) {
                    val previous = lastOrNull()
                    add(characters.filterNot { it == previous }.random(random))
                }
            }
        }

        val exercises = targets.mapIndexed { index, target ->
            PracticeExercise(
                target = target,
                type = exerciseType(mode, index),
                options = createOptions(target, characters, level.optionCount, random),
            )
        }

        return PracticeSession(level = level, mode = mode, exercises = exercises)
    }

    private fun exerciseType(mode: PracticeMode, index: Int): PracticeExerciseType = when (mode) {
        PracticeMode.SignToCharacter -> PracticeExerciseType.SignToCharacter
        PracticeMode.CharacterToSign -> PracticeExerciseType.CharacterToSign
        PracticeMode.Mixed -> if (index % 2 == 0) {
            PracticeExerciseType.SignToCharacter
        } else {
            PracticeExerciseType.CharacterToSign
        }
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
