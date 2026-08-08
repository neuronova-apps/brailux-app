package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.braille.BrailleRepository
import kotlin.random.Random

object PracticeSessionGenerator {
    fun generateDaily(
        random: Random = Random.Default,
    ): PracticeSession {
        val characters = BrailleRepository.getLevel2Characters()
        val targets = characters.shuffled(random).take(PracticeLevel.Daily.exerciseCount)
        val exercises = targets.mapIndexed { index, target ->
            PracticeExercise(
                target = target,
                type = exerciseType(PracticeMode.Mixed, index),
                options = createPedagogicalOptions(
                    target = target,
                    characters = characters,
                    optionCount = PracticeLevel.Daily.optionCount,
                    random = random,
                ),
            )
        }
        return PracticeSession(
            level = PracticeLevel.Daily,
            mode = PracticeMode.Mixed,
            exercises = exercises,
        )
    }

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

    fun generateCustom(
        configuration: CustomPracticeConfiguration,
        random: Random = Random.Default,
    ): PracticeSession {
        val characters = charactersFor(configuration.selectedContentGroups)
        val targets = characters.shuffled(random).take(configuration.exerciseCount.value)
        require(targets.size == configuration.exerciseCount.value) {
            "Selected content must provide enough distinct exercises."
        }
        val exercises = targets.mapIndexed { index, target ->
            PracticeExercise(
                target = target,
                type = exerciseType(configuration.mode, index),
                options = createPedagogicalOptions(
                    target = target,
                    characters = characters,
                    optionCount = PracticeLevel.Custom.optionCount,
                    random = random,
                ),
            )
        }
        return PracticeSession(
            level = PracticeLevel.Custom,
            mode = configuration.mode,
            exercises = exercises,
            customConfiguration = configuration,
        )
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
            PracticeLevel.Custom -> error("Custom sessions require an explicit configuration.")
            PracticeLevel.Daily -> error("Daily sessions use the balanced daily generator.")
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
            PracticeLevel.Custom -> error("Custom sessions require an explicit configuration.")
            PracticeLevel.Daily -> error("Daily sessions use the balanced daily generator.")
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

    private fun charactersFor(
        groups: Set<PracticeContentGroup>,
    ): List<BrailleCharacter> = buildList {
        if (PracticeContentGroup.SpanishAlphabet in groups) {
            addAll(BrailleRepository.getLevel2Characters())
        }
        // Additional groups remain unavailable until their complete repertoires are verified.
    }.distinctBy { it.printedCharacter }

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

    private fun createPedagogicalOptions(
        target: BrailleCharacter,
        characters: List<BrailleCharacter>,
        optionCount: Int,
        random: Random,
    ): List<BrailleCharacter> {
        val targetIndex = characters.indexOf(target)
        val distractors = characters
            .filterNot { it.printedCharacter == target.printedCharacter }
            .sortedBy { candidate ->
                val distance = kotlin.math.abs(characters.indexOf(candidate) - targetIndex)
                distance * 10 + random.nextInt(10)
            }
            .take(optionCount - 1)
        return (distractors + target).shuffled(random)
    }
}
