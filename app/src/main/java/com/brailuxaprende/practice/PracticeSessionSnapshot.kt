package com.brailuxaprende.practice

enum class PracticeSessionPhase {
    Active,
    AwaitingCredit,
    CreditFailed,
    Credited,
}

data class PracticeSessionSnapshot(
    val state: PracticeSessionState,
    val phase: PracticeSessionPhase = PracticeSessionPhase.Active,
    val creditAttempt: Int = 0,
    val engagementReward: EngagementReward? = null,
    val version: Int = CURRENT_VERSION,
) {
    init {
        require(version == CURRENT_VERSION) { "Unsupported practice session snapshot version." }
        require(isValidPracticeSessionId(state.sessionId)) { "Practice session ID is invalid." }
        require(creditAttempt >= 0) { "Credit attempt cannot be negative." }
        validateRestorablePracticeSessionState(state)
        require((phase == PracticeSessionPhase.Active) != state.isCompleted) {
            "Only a completed session can enter the credit workflow."
        }
        require(engagementReward == null || phase == PracticeSessionPhase.Credited) {
            "An engagement reward belongs only to an accredited session."
        }
        engagementReward?.let { reward ->
            require(reward.xpEarned >= 0)
            require(reward.weeklyPracticeDays >= 0)
            require(reward.currentStreak >= 0)
        }
    }

    val sessionId: String
        get() = state.sessionId

    val level: PracticeLevel
        get() = state.session.level

    val summary: PracticeSessionSummary?
        get() = state.takeIf { it.isCompleted }?.summary()

    companion object {
        const val CURRENT_VERSION: Int = 1
    }
}

internal fun validateRestorablePracticeSessionState(state: PracticeSessionState) {
    val exercises = state.session.exercises
    require(state.currentExerciseIndex in exercises.indices) { "Current exercise index is invalid." }
    require(state.attemptsOnCurrentExercise >= 0)
    require(state.firstAttemptCorrect in 0..exercises.size)
    require(state.errors >= 0)
    require(state.currentFirstAttemptCorrectStreak in 0..state.firstAttemptCorrect)
    require(state.longestFirstAttemptCorrectStreak in 0..state.firstAttemptCorrect)
    require(state.currentFirstAttemptCorrectStreak <= state.longestFirstAttemptCorrectStreak)
    require(state.revealedHintCount in 0..state.availableHints.size)
    require(state.hintsUsed >= 0)
    state.session.level.hintLimit?.let { limit -> require(state.hintsUsed <= limit) }
    require(state.currentExerciseAnswers.size == state.attemptsOnCurrentExercise) {
        "Current answer history must match the attempt count."
    }

    val expectedCompletedCount = if (state.isCompleted) exercises.size else state.currentExerciseIndex
    require(state.completedAnswers.size == expectedCompletedCount) {
        "Completed answer history does not match session progress."
    }
    state.completedAnswers.forEachIndexed { expectedIndex, answer ->
        require(answer.exerciseIndex == expectedIndex) { "Completed answers must be contiguous." }
        validateResponses(exercises[expectedIndex], answer.responses, mustBeCorrect = true)
    }

    val currentExercise = exercises[state.currentExerciseIndex]
    state.currentExerciseAnswers.forEach { response ->
        require(currentExercise.options.any { it.printedCharacter == response }) {
            "Current response is not an exercise option."
        }
    }
    state.selectedCharacter?.let { selected ->
        require(currentExercise.options.any { it.printedCharacter == selected }) {
            "Selected character is not an exercise option."
        }
    }

    when (state.validation) {
        PracticeValidationState.AwaitingAnswer -> require(
            state.currentExerciseAnswers.none { it == currentExercise.target.printedCharacter },
        ) { "An awaiting exercise cannot already contain a correct response." }
        PracticeValidationState.Correct -> {
            require(state.selectedCharacter == currentExercise.target.printedCharacter)
            validateResponses(currentExercise, state.currentExerciseAnswers, mustBeCorrect = true)
        }
        PracticeValidationState.Incorrect -> {
            require(state.currentExerciseAnswers.isNotEmpty())
            require(state.currentExerciseAnswers.last() == state.selectedCharacter)
            require(state.selectedCharacter != currentExercise.target.printedCharacter)
            require(state.currentExerciseAnswers.none { it == currentExercise.target.printedCharacter })
        }
    }

    require(!state.isCompleted || (
        state.currentExerciseIndex == exercises.lastIndex &&
            state.validation == PracticeValidationState.Correct
        )) { "A completed session must finish on a correct last exercise." }

    val completedErrors = state.completedAnswers.sumOf { answer ->
        answer.responses.count { response ->
            response != exercises[answer.exerciseIndex].target.printedCharacter
        }
    }
    val currentErrors = if (state.isCompleted) 0 else {
        state.currentExerciseAnswers.count { it != currentExercise.target.printedCharacter }
    }
    val calculatedErrors = completedErrors + currentErrors
    require(calculatedErrors == state.errors) { "Stored error count does not match answer history." }

    val completedFirstAttempts = state.completedAnswers.count { it.responses.size == 1 }
    val currentFirstAttempt = if (
        !state.isCompleted &&
        state.validation == PracticeValidationState.Correct &&
        state.currentExerciseAnswers.size == 1
    ) 1 else 0
    require(completedFirstAttempts + currentFirstAttempt == state.firstAttemptCorrect) {
        "Stored first-attempt count does not match answer history."
    }
}

internal fun PracticeSessionSnapshot?.acceptsCreditResolution(
    resolution: PracticeSessionSnapshot,
): Boolean = this != null &&
    sessionId == resolution.sessionId &&
    level == resolution.level &&
    phase == PracticeSessionPhase.AwaitingCredit &&
    creditAttempt == resolution.creditAttempt &&
    resolution.phase in setOf(
        PracticeSessionPhase.CreditFailed,
        PracticeSessionPhase.Credited,
    )

private fun validateResponses(
    exercise: PracticeExercise,
    responses: List<Char>,
    mustBeCorrect: Boolean,
) {
    require(responses.isNotEmpty()) { "A completed answer needs at least one response." }
    require(responses.all { response ->
        exercise.options.any { it.printedCharacter == response }
    }) { "Answer history contains a response outside the exercise options." }
    if (mustBeCorrect) {
        require(responses.last() == exercise.target.printedCharacter)
        require(responses.dropLast(1).none { it == exercise.target.printedCharacter })
    }
}
