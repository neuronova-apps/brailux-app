package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCharacter

enum class PracticeLevel(
    val exerciseCount: Int,
    val optionCount: Int,
    val showPointNumbersByDefault: Boolean,
    val allowsPointNumberToggle: Boolean,
    val hintLimit: Int?,
) {
    Daily(
        exerciseCount = 5,
        optionCount = 4,
        showPointNumbersByDefault = true,
        allowsPointNumberToggle = true,
        hintLimit = null,
    ),
    BrailleExplorer(
        exerciseCount = 10,
        optionCount = 4,
        showPointNumbersByDefault = true,
        allowsPointNumberToggle = true,
        hintLimit = null,
    ),
    BrailleRecognizer(
        exerciseCount = 15,
        optionCount = 6,
        showPointNumbersByDefault = false,
        allowsPointNumberToggle = true,
        hintLimit = 3,
    ),
    BrailleChallenge(
        exerciseCount = 20,
        optionCount = 6,
        showPointNumbersByDefault = false,
        allowsPointNumberToggle = false,
        hintLimit = 0,
    ),
    Custom(
        exerciseCount = 10,
        optionCount = 6,
        showPointNumbersByDefault = true,
        allowsPointNumberToggle = true,
        hintLimit = null,
    ),
}

enum class PracticeExerciseType {
    SignToCharacter,
    CharacterToSign,
}

enum class PracticeMode {
    SignToCharacter,
    CharacterToSign,
    Mixed,
}

enum class PracticeContentGroup(
    val isAvailable: Boolean,
) {
    SpanishAlphabet(isAvailable = true),
    AccentuationAndDiaeresis(isAvailable = false),
    Punctuation(isAvailable = false),
    Numbers(isAvailable = false),
    Capitals(isAvailable = false),
}

enum class CustomExerciseCount(
    val value: Int,
) {
    Ten(10),
    Fifteen(15),
    Twenty(20),
    ;

    companion object {
        fun fromValue(value: Int): CustomExerciseCount = entries.firstOrNull { it.value == value } ?: Ten
    }
}

data class CustomPracticeConfiguration(
    val additionalContentGroups: Set<PracticeContentGroup> = emptySet(),
    val exerciseCount: CustomExerciseCount = CustomExerciseCount.Ten,
    val mode: PracticeMode = PracticeMode.SignToCharacter,
    val hintsEnabled: Boolean = true,
    val showPointNumbers: Boolean = true,
) {
    init {
        require(PracticeContentGroup.SpanishAlphabet !in additionalContentGroups) {
            "The Spanish alphabet is mandatory and must not be stored as an additional group."
        }
        require(additionalContentGroups.all { it.isAvailable }) {
            "Only verified and available Braille content groups can be selected."
        }
    }

    val selectedContentGroups: Set<PracticeContentGroup>
        get() = setOf(PracticeContentGroup.SpanishAlphabet) + additionalContentGroups

    fun withContentGroup(
        group: PracticeContentGroup,
        selected: Boolean,
    ): CustomPracticeConfiguration {
        if (group == PracticeContentGroup.SpanishAlphabet || !group.isAvailable) return this
        return copy(
            additionalContentGroups = if (selected) {
                additionalContentGroups + group
            } else {
                additionalContentGroups - group
            },
        )
    }

    fun selectAllAvailableAdditional(): CustomPracticeConfiguration = copy(
        additionalContentGroups = PracticeContentGroup.entries
            .filter { it != PracticeContentGroup.SpanishAlphabet && it.isAvailable }
            .toSet(),
    )

    fun removeAdditional(): CustomPracticeConfiguration = copy(additionalContentGroups = emptySet())
}

enum class BrailleRow {
    Top,
    Middle,
    Bottom,
}

sealed interface PracticeHint {
    data class ActivePointCount(
        val count: Int,
    ) : PracticeHint {
        init {
            require(count in 0..6) { "Active point count must be between 0 and 6." }
        }
    }

    data class ColumnDistribution(
        val leftCount: Int,
        val rightCount: Int,
    ) : PracticeHint {
        init {
            require(leftCount in 0..3) { "Left column point count must be between 0 and 3." }
            require(rightCount in 0..3) { "Right column point count must be between 0 and 3." }
        }
    }

    data class RowState(
        val row: BrailleRow,
        val activeCount: Int,
    ) : PracticeHint {
        init {
            require(activeCount in 0..2) { "Row point count must be between 0 and 2." }
        }
    }

    data class PointState(
        val point: Int,
        val isActive: Boolean,
    ) : PracticeHint {
        init {
            require(point in 1..6) { "Braille point must be between 1 and 6." }
        }
    }

    data class CharacterCategory(
        val isVowel: Boolean,
    ) : PracticeHint

    data class AlphabetRange(
        val first: Char,
        val last: Char,
    ) : PracticeHint

    data class AlphabetComparison(
        val reference: Char,
        val targetComesAfter: Boolean,
    ) : PracticeHint
}

data class PracticeExercise(
    val target: BrailleCharacter,
    val type: PracticeExerciseType,
    val options: List<BrailleCharacter>,
) {
    init {
        require(target in options) { "Exercise options must contain the correct answer." }
        require(options.map { it.printedCharacter }.distinct().size == options.size) {
            "Exercise options must not contain duplicate characters."
        }
    }
}

data class PracticeSession(
    val level: PracticeLevel,
    val mode: PracticeMode,
    val exercises: List<PracticeExercise>,
    val customConfiguration: CustomPracticeConfiguration? = null,
) {
    init {
        val expectedExerciseCount = customConfiguration?.exerciseCount?.value ?: level.exerciseCount
        require(exercises.size == expectedExerciseCount) {
            "Session exercise count must match the configured level."
        }
        require((level == PracticeLevel.Custom) == (customConfiguration != null)) {
            "Only custom sessions can contain a custom configuration."
        }
        when (mode) {
            PracticeMode.SignToCharacter -> require(
                exercises.all { it.type == PracticeExerciseType.SignToCharacter },
            ) { "Sign-to-character sessions must keep the selected exercise type." }
            PracticeMode.CharacterToSign -> require(
                exercises.all { it.type == PracticeExerciseType.CharacterToSign },
            ) { "Character-to-sign sessions must keep the selected exercise type." }
            PracticeMode.Mixed -> require(
                exercises.map { it.type }.toSet() == PracticeExerciseType.entries.toSet(),
            ) { "Mixed sessions must contain both exercise types." }
        }
    }

    val hintsEnabled: Boolean
        get() = customConfiguration?.hintsEnabled ?: (level.hintLimit != 0)

    val allowsPointNumberToggle: Boolean
        get() = customConfiguration == null && level.allowsPointNumberToggle

    val initialPointNumberVisibility: Boolean
        get() = customConfiguration?.showPointNumbers ?: level.showPointNumbersByDefault
}

data class PracticeSessionSummary(
    val exercisesCompleted: Int,
    val firstAttemptCorrect: Int,
    val errors: Int,
    val accuracyPercentage: Int,
    val practicedLetters: List<Char>,
    val hintsUsed: Int = 0,
    val practicedContentGroups: Set<PracticeContentGroup> = setOf(
        PracticeContentGroup.SpanishAlphabet,
    ),
    val mode: PracticeMode? = null,
    val longestFirstAttemptCorrectStreak: Int = 0,
    val sessionId: String = newPracticeSessionId(),
)
