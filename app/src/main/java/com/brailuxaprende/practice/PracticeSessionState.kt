package com.brailuxaprende.practice

enum class PracticeValidationState {
    AwaitingAnswer,
    Correct,
    Incorrect,
}

data class PracticeSessionState(
    val session: PracticeSession,
    val currentExerciseIndex: Int = 0,
    val selectedCharacter: Char? = null,
    val attemptsOnCurrentExercise: Int = 0,
    val firstAttemptCorrect: Int = 0,
    val errors: Int = 0,
    val validation: PracticeValidationState = PracticeValidationState.AwaitingAnswer,
    val showPointNumbers: Boolean = session.level.showPointNumbersByDefault,
    val hintVisible: Boolean = false,
    val hintsUsed: Int = 0,
    val isCompleted: Boolean = false,
) {
    val currentExercise: PracticeExercise
        get() = session.exercises[currentExerciseIndex]

    val exerciseNumber: Int
        get() = currentExerciseIndex + 1

    val completedExercises: Int
        get() = when {
            isCompleted -> session.exercises.size
            validation == PracticeValidationState.Correct -> currentExerciseIndex + 1
            else -> currentExerciseIndex
        }

    val accuracyPercentage: Int
        get() = if (session.exercises.isEmpty()) {
            0
        } else {
            firstAttemptCorrect * 100 / session.exercises.size
        }

    val hintsRemaining: Int?
        get() = session.level.hintLimit?.let { limit -> (limit - hintsUsed).coerceAtLeast(0) }

    fun selectAnswer(character: Char): PracticeSessionState {
        if (validation == PracticeValidationState.Correct || isCompleted) return this
        require(currentExercise.options.any { it.printedCharacter == character }) {
            "Selected answer must be one of the exercise options."
        }
        return copy(
            selectedCharacter = character,
            validation = PracticeValidationState.AwaitingAnswer,
        )
    }

    fun checkAnswer(): PracticeSessionState {
        val answer = selectedCharacter ?: return this
        if (validation == PracticeValidationState.Correct || isCompleted) return this

        val isCorrect = answer == currentExercise.target.printedCharacter
        return if (isCorrect) {
            copy(
                attemptsOnCurrentExercise = attemptsOnCurrentExercise + 1,
                firstAttemptCorrect = firstAttemptCorrect + if (attemptsOnCurrentExercise == 0) 1 else 0,
                validation = PracticeValidationState.Correct,
            )
        } else {
            copy(
                attemptsOnCurrentExercise = attemptsOnCurrentExercise + 1,
                errors = errors + 1,
                validation = PracticeValidationState.Incorrect,
            )
        }
    }

    fun nextExercise(): PracticeSessionState {
        if (validation != PracticeValidationState.Correct || isCompleted) return this
        if (currentExerciseIndex == session.exercises.lastIndex) {
            return copy(isCompleted = true)
        }

        return copy(
            currentExerciseIndex = currentExerciseIndex + 1,
            selectedCharacter = null,
            attemptsOnCurrentExercise = 0,
            validation = PracticeValidationState.AwaitingAnswer,
            hintVisible = false,
        )
    }

    fun togglePointNumbers(): PracticeSessionState = copy(showPointNumbers = !showPointNumbers)

    fun showHint(): PracticeSessionState {
        if (hintVisible || hintsRemaining == 0) return this
        return copy(
            hintVisible = true,
            hintsUsed = hintsUsed + if (session.level.hintLimit == null) 0 else 1,
        )
    }

    fun summary(): PracticeSessionSummary {
        require(isCompleted) { "A summary is only available after completing the session." }
        return PracticeSessionSummary(
            exercisesCompleted = session.exercises.size,
            firstAttemptCorrect = firstAttemptCorrect,
            errors = errors,
            accuracyPercentage = accuracyPercentage,
            practicedLetters = session.exercises
                .map { it.target.printedCharacter }
                .distinct()
                .sorted(),
            hintsUsed = hintsUsed,
        )
    }
}
