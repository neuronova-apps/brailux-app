package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCharacter

enum class PracticeLevel(val exerciseCount: Int) {
    BrailleExplorer(exerciseCount = 10),
}

enum class PracticeExerciseType {
    SignToCharacter,
    CharacterToSign,
}

enum class PracticeHint {
    OnePoint,
    TwoPoints,
    ThreePoints,
    FourPoints,
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

    val hint: PracticeHint
        get() = when (target.cell.activePoints().size) {
            1 -> PracticeHint.OnePoint
            2 -> PracticeHint.TwoPoints
            3 -> PracticeHint.ThreePoints
            else -> PracticeHint.FourPoints
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
)
