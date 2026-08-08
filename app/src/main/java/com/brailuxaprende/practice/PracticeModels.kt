package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCharacter

enum class PracticeLevel(
    val exerciseCount: Int,
    val optionCount: Int,
    val showPointNumbersByDefault: Boolean,
    val allowsPointNumberToggle: Boolean,
    val hintLimit: Int?,
) {
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
}

enum class PracticeExerciseType {
    SignToCharacter,
    CharacterToSign,
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
    val exercises: List<PracticeExercise>,
) {
    init {
        require(exercises.size == level.exerciseCount) {
            "Session exercise count must match the configured level."
        }
    }
}

data class PracticeSessionSummary(
    val exercisesCompleted: Int,
    val firstAttemptCorrect: Int,
    val errors: Int,
    val accuracyPercentage: Int,
    val practicedLetters: List<Char>,
    val hintsUsed: Int = 0,
)
