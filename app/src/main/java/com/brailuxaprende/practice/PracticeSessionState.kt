package com.brailuxaprende.practice

enum class PracticeValidationState {
    AwaitingAnswer,
    Correct,
    Incorrect,
}

data class PracticeCompletedAnswer(
    val exerciseIndex: Int,
    val responses: List<Char>,
)

data class PracticeSessionState(
    val session: PracticeSession,
    val currentExerciseIndex: Int = 0,
    val selectedCharacter: Char? = null,
    val attemptsOnCurrentExercise: Int = 0,
    val firstAttemptCorrect: Int = 0,
    val errors: Int = 0,
    val currentFirstAttemptCorrectStreak: Int = 0,
    val longestFirstAttemptCorrectStreak: Int = 0,
    val validation: PracticeValidationState = PracticeValidationState.AwaitingAnswer,
    val showPointNumbers: Boolean = session.initialPointNumberVisibility,
    val revealedHintCount: Int = 0,
    val hintsUsed: Int = 0,
    val isCompleted: Boolean = false,
    val completedAnswers: List<PracticeCompletedAnswer> = emptyList(),
    val currentExerciseAnswers: List<Char> = emptyList(),
    val sessionId: String = newPracticeSessionId(),
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

    val availableHints: List<PracticeHint>
        get() = if (session.hintsEnabled) {
            PracticeHintGenerator.generate(session.level, currentExercise)
        } else {
            emptyList()
        }

    val visibleHints: List<PracticeHint>
        get() = availableHints.take(revealedHintCount)

    val hintVisible: Boolean
        get() = visibleHints.isNotEmpty()

    val canShowHint: Boolean
        get() = !isCompleted &&
            validation != PracticeValidationState.Correct &&
            revealedHintCount < availableHints.size &&
            hintsRemaining != 0

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
            val firstAttempt = attemptsOnCurrentExercise == 0
            val updatedStreak = if (firstAttempt) currentFirstAttemptCorrectStreak + 1 else 0
            copy(
                attemptsOnCurrentExercise = attemptsOnCurrentExercise + 1,
                currentExerciseAnswers = currentExerciseAnswers + answer,
                firstAttemptCorrect = firstAttemptCorrect + if (firstAttempt) 1 else 0,
                currentFirstAttemptCorrectStreak = updatedStreak,
                longestFirstAttemptCorrectStreak = maxOf(
                    longestFirstAttemptCorrectStreak,
                    updatedStreak,
                ),
                validation = PracticeValidationState.Correct,
            )
        } else {
            copy(
                attemptsOnCurrentExercise = attemptsOnCurrentExercise + 1,
                currentExerciseAnswers = currentExerciseAnswers + answer,
                errors = errors + 1,
                currentFirstAttemptCorrectStreak = 0,
                validation = PracticeValidationState.Incorrect,
            )
        }
    }

    fun nextExercise(): PracticeSessionState {
        if (validation != PracticeValidationState.Correct || isCompleted) return this
        val completedAnswer = PracticeCompletedAnswer(
            exerciseIndex = currentExerciseIndex,
            responses = currentExerciseAnswers,
        )
        val updatedCompletedAnswers = completedAnswers + completedAnswer
        if (currentExerciseIndex == session.exercises.lastIndex) {
            return copy(
                isCompleted = true,
                completedAnswers = updatedCompletedAnswers,
            )
        }

        return copy(
            currentExerciseIndex = currentExerciseIndex + 1,
            selectedCharacter = null,
            attemptsOnCurrentExercise = 0,
            validation = PracticeValidationState.AwaitingAnswer,
            revealedHintCount = 0,
            completedAnswers = updatedCompletedAnswers,
            currentExerciseAnswers = emptyList(),
        )
    }

    fun togglePointNumbers(): PracticeSessionState {
        if (!session.allowsPointNumberToggle) return this
        return copy(showPointNumbers = !showPointNumbers)
    }

    fun showHint(): PracticeSessionState {
        if (!canShowHint) return this
        return copy(
            revealedHintCount = revealedHintCount + 1,
            hintsUsed = hintsUsed + if (
                session.level == PracticeLevel.BrailleExplorer
            ) 0 else 1,
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
            practicedContentGroups = session.customConfiguration?.selectedContentGroups
                ?: setOf(PracticeContentGroup.SpanishAlphabet),
            mode = session.mode,
            longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
            sessionId = sessionId,
        )
    }
}
