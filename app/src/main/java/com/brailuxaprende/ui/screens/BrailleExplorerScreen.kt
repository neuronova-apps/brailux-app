package com.brailuxaprende.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.practice.BrailleRow
import com.brailuxaprende.practice.CustomPracticeConfiguration
import com.brailuxaprende.practice.EngagementReward
import com.brailuxaprende.practice.PracticeContentGroup
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.PracticeExercise
import com.brailuxaprende.practice.PracticeExerciseType
import com.brailuxaprende.practice.PracticeHint
import com.brailuxaprende.practice.PracticeLevel
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.practice.PracticeSession
import com.brailuxaprende.practice.PracticeSessionGenerator
import com.brailuxaprende.practice.PracticeSessionPhase
import com.brailuxaprende.practice.PracticeSessionSnapshot
import com.brailuxaprende.practice.PracticeSessionState
import com.brailuxaprende.practice.PracticeSessionSummary
import com.brailuxaprende.practice.PracticeValidationState
import com.brailuxaprende.practice.SystemPracticeClock
import com.brailuxaprende.practice.dailyChallengeSessionId
import com.brailuxaprende.practice.dailyPracticeSessionId
import com.brailuxaprende.practice.newPracticeSessionId
import com.brailuxaprende.ui.components.BrailleCellView
import com.brailuxaprende.ui.components.BrailuxBackButton
import com.brailuxaprende.ui.components.BrailuxFeedbackCard
import com.brailuxaprende.ui.components.BrailuxFeedbackType
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard
import com.brailuxaprende.ui.theme.BrailuxTheme

private enum class AnswerResult {
    Correct,
    Incorrect,
}

@Composable
fun DailyPracticeScreen(
    date: PracticeDate = SystemPracticeClock.today(),
    learningProgress: LearningProgress = LearningProgress(),
    practiceProgress: PracticeProgress = PracticeProgress(),
    onSessionCompleted: (
        PracticeSessionSummary,
        onRecorded: (EngagementReward?) -> Unit,
    ) -> Unit,
    onBackToHome: () -> Unit,
    storedSnapshot: PracticeSessionSnapshot?,
    sessionsLoaded: Boolean,
    onSnapshotChanged: (PracticeSessionSnapshot) -> Unit,
    onSnapshotReadyForCredit: (
        PracticeSessionSnapshot,
        onPersisted: (Boolean) -> Unit,
    ) -> Unit,
    onSnapshotCreditResolved: (PracticeSessionSnapshot) -> Unit,
    onSnapshotCleared: (PracticeLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillePracticeLevelScreen(
        level = PracticeLevel.Daily,
        sessionFactory = {
            PracticeSessionGenerator.generateDaily(
                date = date,
                learningProgress = learningProgress,
                practiceProgress = practiceProgress,
            )
        },
        expectedSessionId = dailyPracticeSessionId(date),
        onDailySessionCompleted = onSessionCompleted,
        onBackToPractice = onBackToHome,
        storedSnapshot = storedSnapshot,
        sessionsLoaded = sessionsLoaded,
        onSnapshotChanged = onSnapshotChanged,
        onSnapshotReadyForCredit = onSnapshotReadyForCredit,
        onSnapshotCreditResolved = onSnapshotCreditResolved,
        onSnapshotCleared = onSnapshotCleared,
        modifier = modifier,
    )
}

@Composable
fun DailyChallengeScreen(
    date: PracticeDate = SystemPracticeClock.today(),
    learningProgress: LearningProgress = LearningProgress(),
    practiceProgress: PracticeProgress = PracticeProgress(),
    onSessionCompleted: (
        PracticeSessionSummary,
        onRecorded: (EngagementReward?) -> Unit,
    ) -> Unit,
    onBackToHome: () -> Unit,
    storedSnapshot: PracticeSessionSnapshot?,
    sessionsLoaded: Boolean,
    onSnapshotChanged: (PracticeSessionSnapshot) -> Unit,
    onSnapshotReadyForCredit: (
        PracticeSessionSnapshot,
        onPersisted: (Boolean) -> Unit,
    ) -> Unit,
    onSnapshotCreditResolved: (PracticeSessionSnapshot) -> Unit,
    onSnapshotCleared: (PracticeLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillePracticeLevelScreen(
        level = PracticeLevel.DailyChallenge,
        sessionFactory = {
            PracticeSessionGenerator.generateDailyChallenge(
                date = date,
                learningProgress = learningProgress,
                practiceProgress = practiceProgress,
            )
        },
        expectedSessionId = dailyChallengeSessionId(date),
        onDailySessionCompleted = onSessionCompleted,
        onBackToPractice = onBackToHome,
        storedSnapshot = storedSnapshot,
        sessionsLoaded = sessionsLoaded,
        onSnapshotChanged = onSnapshotChanged,
        onSnapshotReadyForCredit = onSnapshotReadyForCredit,
        onSnapshotCreditResolved = onSnapshotCreditResolved,
        onSnapshotCleared = onSnapshotCleared,
        modifier = modifier,
    )
}

@Composable
fun BrailleExplorerScreen(
    mode: PracticeMode,
    onSessionCompleted: (PracticeSessionSummary, onRecorded: (EngagementReward?) -> Unit) -> Unit,
    onBackToPractice: () -> Unit,
    storedSnapshot: PracticeSessionSnapshot?,
    sessionsLoaded: Boolean,
    onSnapshotChanged: (PracticeSessionSnapshot) -> Unit,
    onSnapshotReadyForCredit: (
        PracticeSessionSnapshot,
        onPersisted: (Boolean) -> Unit,
    ) -> Unit,
    onSnapshotCreditResolved: (PracticeSessionSnapshot) -> Unit,
    onSnapshotCleared: (PracticeLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillePracticeLevelScreen(
        level = PracticeLevel.BrailleExplorer,
        sessionFactory = {
            PracticeSessionGenerator.generate(mode)
        },
        mode = mode,
        onSessionCompleted = onSessionCompleted,
        onBackToPractice = onBackToPractice,
        storedSnapshot = storedSnapshot,
        sessionsLoaded = sessionsLoaded,
        onSnapshotChanged = onSnapshotChanged,
        onSnapshotReadyForCredit = onSnapshotReadyForCredit,
        onSnapshotCreditResolved = onSnapshotCreditResolved,
        onSnapshotCleared = onSnapshotCleared,
        modifier = modifier,
    )
}

@Composable
fun BrailleRecognizerScreen(
    mode: PracticeMode,
    onSessionCompleted: (PracticeSessionSummary, onRecorded: (EngagementReward?) -> Unit) -> Unit,
    onBackToPractice: () -> Unit,
    storedSnapshot: PracticeSessionSnapshot?,
    sessionsLoaded: Boolean,
    onSnapshotChanged: (PracticeSessionSnapshot) -> Unit,
    onSnapshotReadyForCredit: (
        PracticeSessionSnapshot,
        onPersisted: (Boolean) -> Unit,
    ) -> Unit,
    onSnapshotCreditResolved: (PracticeSessionSnapshot) -> Unit,
    onSnapshotCleared: (PracticeLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillePracticeLevelScreen(
        level = PracticeLevel.BrailleRecognizer,
        sessionFactory = {
            PracticeSessionGenerator.generateLevel2(mode)
        },
        mode = mode,
        onSessionCompleted = onSessionCompleted,
        onBackToPractice = onBackToPractice,
        storedSnapshot = storedSnapshot,
        sessionsLoaded = sessionsLoaded,
        onSnapshotChanged = onSnapshotChanged,
        onSnapshotReadyForCredit = onSnapshotReadyForCredit,
        onSnapshotCreditResolved = onSnapshotCreditResolved,
        onSnapshotCleared = onSnapshotCleared,
        modifier = modifier,
    )
}

@Composable
fun BrailleChallengeScreen(
    mode: PracticeMode,
    onSessionCompleted: (PracticeSessionSummary, onRecorded: (EngagementReward?) -> Unit) -> Unit,
    onBackToPractice: () -> Unit,
    storedSnapshot: PracticeSessionSnapshot?,
    sessionsLoaded: Boolean,
    onSnapshotChanged: (PracticeSessionSnapshot) -> Unit,
    onSnapshotReadyForCredit: (
        PracticeSessionSnapshot,
        onPersisted: (Boolean) -> Unit,
    ) -> Unit,
    onSnapshotCreditResolved: (PracticeSessionSnapshot) -> Unit,
    onSnapshotCleared: (PracticeLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillePracticeLevelScreen(
        level = PracticeLevel.BrailleChallenge,
        sessionFactory = {
            PracticeSessionGenerator.generateLevel3(mode)
        },
        mode = mode,
        onSessionCompleted = onSessionCompleted,
        onBackToPractice = onBackToPractice,
        storedSnapshot = storedSnapshot,
        sessionsLoaded = sessionsLoaded,
        onSnapshotChanged = onSnapshotChanged,
        onSnapshotReadyForCredit = onSnapshotReadyForCredit,
        onSnapshotCreditResolved = onSnapshotCreditResolved,
        onSnapshotCleared = onSnapshotCleared,
        modifier = modifier,
    )
}

@Composable
fun CustomBraillePracticeScreen(
    configuration: CustomPracticeConfiguration,
    onSessionCompleted: (PracticeSessionSummary, onRecorded: (EngagementReward?) -> Unit) -> Unit,
    onChangeConfiguration: () -> Unit,
    onBackToPractice: () -> Unit,
    storedSnapshot: PracticeSessionSnapshot?,
    sessionsLoaded: Boolean,
    onSnapshotChanged: (PracticeSessionSnapshot) -> Unit,
    onSnapshotReadyForCredit: (
        PracticeSessionSnapshot,
        onPersisted: (Boolean) -> Unit,
    ) -> Unit,
    onSnapshotCreditResolved: (PracticeSessionSnapshot) -> Unit,
    onSnapshotCleared: (PracticeLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillePracticeLevelScreen(
        level = PracticeLevel.Custom,
        sessionFactory = {
            PracticeSessionGenerator.generateCustom(configuration)
        },
        onSessionCompleted = onSessionCompleted,
        onBackToPractice = onBackToPractice,
        customConfiguration = configuration,
        onChangeConfiguration = onChangeConfiguration,
        storedSnapshot = storedSnapshot,
        sessionsLoaded = sessionsLoaded,
        onSnapshotChanged = onSnapshotChanged,
        onSnapshotReadyForCredit = onSnapshotReadyForCredit,
        onSnapshotCreditResolved = onSnapshotCreditResolved,
        onSnapshotCleared = onSnapshotCleared,
        modifier = modifier,
    )
}

@Composable
private fun BraillePracticeLevelScreen(
    level: PracticeLevel,
    sessionFactory: () -> PracticeSession,
    expectedSessionId: String? = null,
    onSessionCompleted: (
        (PracticeSessionSummary, onRecorded: (EngagementReward?) -> Unit) -> Unit
    )? = null,
    onDailySessionCompleted: (
        (PracticeSessionSummary, onRecorded: (EngagementReward?) -> Unit) -> Unit
    )? = null,
    onBackToPractice: () -> Unit,
    mode: PracticeMode? = null,
    customConfiguration: CustomPracticeConfiguration? = null,
    onChangeConfiguration: (() -> Unit)? = null,
    storedSnapshot: PracticeSessionSnapshot?,
    sessionsLoaded: Boolean,
    onSnapshotChanged: (PracticeSessionSnapshot) -> Unit,
    onSnapshotReadyForCredit: (
        PracticeSessionSnapshot,
        onPersisted: (Boolean) -> Unit,
    ) -> Unit,
    onSnapshotCreditResolved: (PracticeSessionSnapshot) -> Unit,
    onSnapshotCleared: (PracticeLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSnapshotCompatible = storedSnapshot != null &&
        storedSnapshot.level == level &&
        (expectedSessionId == null || storedSnapshot.sessionId == expectedSessionId) &&
        (mode == null || storedSnapshot.state.session.mode == mode) &&
        (customConfiguration == null || storedSnapshot.state.session.customConfiguration == customConfiguration)

    LaunchedEffect(sessionsLoaded, level, mode, customConfiguration, storedSnapshot?.sessionId, expectedSessionId) {
        if (sessionsLoaded && !isSnapshotCompatible) {
            onSnapshotChanged(
                PracticeSessionSnapshot(
                    state = PracticeSessionState(
                        session = sessionFactory(),
                        sessionId = expectedSessionId ?: newPracticeSessionId(),
                    ),
                ),
            )
        }
    }

    val snapshot = storedSnapshot?.takeIf {
        it.level == level &&
            (expectedSessionId == null || it.sessionId == expectedSessionId) &&
            (mode == null || it.state.session.mode == mode) &&
            (customConfiguration == null || it.state.session.customConfiguration == customConfiguration)
    }
    if (!sessionsLoaded || snapshot == null) return

    LaunchedEffect(snapshot.sessionId, snapshot.phase) {
        if (snapshot.phase == PracticeSessionPhase.AwaitingCredit) {
            onSnapshotReadyForCredit(snapshot) { persisted ->
                if (!persisted) {
                    onSnapshotCreditResolved(
                        snapshot.copy(phase = PracticeSessionPhase.CreditFailed),
                    )
                } else if (onDailySessionCompleted != null) {
                    onDailySessionCompleted(requireNotNull(snapshot.summary)) { reward ->
                        onSnapshotCreditResolved(
                            if (reward == null) {
                                snapshot.copy(phase = PracticeSessionPhase.CreditFailed)
                            } else {
                                snapshot.copy(
                                    phase = PracticeSessionPhase.Credited,
                                    engagementReward = reward,
                                )
                            },
                        )
                    }
                } else {
                    requireNotNull(onSessionCompleted)(requireNotNull(snapshot.summary)) { reward ->
                        onSnapshotCreditResolved(
                            if (reward == null) {
                                snapshot.copy(phase = PracticeSessionPhase.CreditFailed)
                            } else {
                                snapshot.copy(
                                    phase = PracticeSessionPhase.Credited,
                                    engagementReward = reward,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    val state = snapshot.state
    if (snapshot.phase == PracticeSessionPhase.Credited) {
        BackHandler {
            onBackToPractice()
            if (level != PracticeLevel.Daily && level != PracticeLevel.DailyChallenge) {
                onSnapshotCleared(level)
            }
        }
        BraillePracticeSummary(
            level = level,
            summary = requireNotNull(snapshot.summary),
            onPracticeAgain = {
                onSnapshotChanged(
                    PracticeSessionSnapshot(
                        state = PracticeSessionState(
                            session = sessionFactory(),
                            sessionId = expectedSessionId ?: newPracticeSessionId(),
                        ),
                    ),
                )
            },
            customConfiguration = state.session.customConfiguration ?: customConfiguration,
            onChangeConfiguration = onChangeConfiguration?.let { changeConfiguration ->
                {
                    changeConfiguration()
                    onSnapshotCleared(level)
                }
            },
            engagementReward = snapshot.engagementReward,
            onBackToPractice = {
                onBackToPractice()
                if (level != PracticeLevel.Daily && level != PracticeLevel.DailyChallenge) {
                    onSnapshotCleared(level)
                }
            },
            modifier = modifier,
        )
    } else {
        BraillePracticeExercise(
            level = level,
            state = state,
            completionPending = snapshot.phase == PracticeSessionPhase.AwaitingCredit,
            completionFailed = snapshot.phase == PracticeSessionPhase.CreditFailed,
            onStateChange = { updatedState ->
                onSnapshotChanged(snapshot.copy(state = updatedState))
            },
            onNext = {
                if (snapshot.phase != PracticeSessionPhase.AwaitingCredit) {
                    val nextState = if (state.isCompleted) state else state.nextExercise()
                    onSnapshotChanged(
                        snapshot.copy(
                            state = nextState,
                            phase = if (nextState.isCompleted) {
                                PracticeSessionPhase.AwaitingCredit
                            } else {
                                PracticeSessionPhase.Active
                            },
                            creditAttempt = if (nextState.isCompleted) {
                                snapshot.creditAttempt + 1
                            } else {
                                snapshot.creditAttempt
                            },
                            engagementReward = null,
                        ),
                    )
                }
            },
            onBack = onBackToPractice,
            modifier = modifier,
        )
    }
}

@Composable
private fun BraillePracticeExercise(
    level: PracticeLevel,
    state: PracticeSessionState,
    completionPending: Boolean,
    completionFailed: Boolean,
    onStateChange: (PracticeSessionState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercise = state.currentExercise
    val progressDescription = stringResource(
        R.string.practice_exercise_count,
        state.exerciseNumber,
        state.session.exercises.size,
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (level == PracticeLevel.Daily) {
                DailyPracticeHeader(
                    onBack = onBack,
                )
            } else if (level == PracticeLevel.DailyChallenge) {
                DailyChallengeHeader(
                    onBack = onBack,
                )
            } else {
                BrailuxScreenHeader(
                    title = stringResource(level.titleResource()),
                    subtitle = stringResource(state.session.mode.titleResource()),
                    onBack = onBack,
                )
                if (level == PracticeLevel.BrailleChallenge && state.currentExerciseIndex == 0) {
                    Spacer(modifier = Modifier.height(18.dp))
                    BrailuxSectionCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.practice_level_3_intro),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = progressDescription,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { state.exerciseNumber.toFloat() / state.session.exercises.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .semantics { contentDescription = progressDescription },
                )
                Text(
                    text = stringResource(
                        R.string.practice_first_attempt_count,
                        state.firstAttemptCorrect,
                    ),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.practice_error_count, state.errors),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (state.session.hintsEnabled) {
                    state.hintsRemaining?.let { hintsRemaining ->
                        Text(
                            text = stringResource(R.string.practice_hints_available, hintsRemaining),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            if (state.session.allowsPointNumberToggle) {
                Spacer(modifier = Modifier.height(14.dp))
                BrailuxSectionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                ) {
                    Text(
                        text = stringResource(R.string.practice_orientation_reading),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.practice_point_orientation),
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    PointNumberToggle(
                        checked = state.showPointNumbers,
                        onCheckedChange = { onStateChange(state.togglePointNumbers()) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                ExercisePrompt(
                    exercise = exercise,
                    showPointNumbers = state.showPointNumbers,
                )
                if (exercise.type == PracticeExerciseType.SignToCharacter) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        exercise.options.forEach { option ->
                            val answerResult = when {
                                state.selectedCharacter != option.printedCharacter -> null
                                state.validation == PracticeValidationState.Correct -> AnswerResult.Correct
                                state.validation == PracticeValidationState.Incorrect -> AnswerResult.Incorrect
                                else -> null
                            }
                            PracticeAnswerOption(
                                option = option,
                                type = exercise.type,
                                selected = state.selectedCharacter == option.printedCharacter,
                                result = answerResult,
                                showPointNumbers = state.showPointNumbers,
                                enabled = state.validation != PracticeValidationState.Correct,
                                onSelect = {
                                    onStateChange(state.selectAnswer(option.printedCharacter))
                                },
                                isCompact = false,
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        exercise.options.chunked(2).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowOptions.forEach { option ->
                                    val answerResult = when {
                                        state.selectedCharacter != option.printedCharacter -> null
                                        state.validation == PracticeValidationState.Correct -> AnswerResult.Correct
                                        state.validation == PracticeValidationState.Incorrect -> AnswerResult.Incorrect
                                        else -> null
                                    }
                                    PracticeAnswerOption(
                                        option = option,
                                        type = exercise.type,
                                        selected = state.selectedCharacter == option.printedCharacter,
                                        result = answerResult,
                                        showPointNumbers = state.showPointNumbers,
                                        enabled = state.validation != PracticeValidationState.Correct,
                                        onSelect = {
                                            onStateChange(state.selectAnswer(option.printedCharacter))
                                        },
                                        modifier = Modifier.weight(1f),
                                        isCompact = true,
                                    )
                                }
                                if (rowOptions.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            if (state.visibleHints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.visibleHints.forEachIndexed { index, hint ->
                        BrailuxFeedbackCard(
                            message = hintText(hint),
                            type = BrailuxFeedbackType.Warning,
                            announceForAccessibility = index == state.visibleHints.lastIndex,
                        )
                    }
                }
            }

            if (state.validation != PracticeValidationState.AwaitingAnswer) {
                Spacer(modifier = Modifier.height(10.dp))
                BrailuxFeedbackCard(
                    message = if (state.validation == PracticeValidationState.Correct) {
                        stringResource(
                            R.string.practice_answer_correct,
                            exercise.target.printedCharacter.toString(),
                        )
                    } else {
                        stringResource(R.string.practice_answer_incorrect)
                    },
                    type = if (state.validation == PracticeValidationState.Correct) {
                        BrailuxFeedbackType.Success
                    } else {
                        BrailuxFeedbackType.Error
                    },
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }

            if (completionFailed) {
                Spacer(modifier = Modifier.height(10.dp))
                BrailuxFeedbackCard(
                    message = stringResource(R.string.practice_progress_save_error),
                    type = BrailuxFeedbackType.Error,
                    announceForAccessibility = true,
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }

            if (state.session.hintsEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                BrailuxSecondaryButton(
                    text = stringResource(R.string.practice_show_hint),
                    onClick = { onStateChange(state.showHint()) },
                    enabled = state.canShowHint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (state.validation == PracticeValidationState.Correct) {
                BrailuxPrimaryButton(
                    text = stringResource(R.string.practice_next_exercise),
                    onClick = onNext,
                    enabled = !completionPending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            } else {
                BrailuxPrimaryButton(
                    text = stringResource(R.string.practice_check_answer),
                    onClick = { onStateChange(state.checkAnswer()) },
                    enabled = state.selectedCharacter != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ColumnScope.ExercisePrompt(
    exercise: PracticeExercise,
    showPointNumbers: Boolean,
) {
    Text(
        text = stringResource(
            if (exercise.type == PracticeExerciseType.SignToCharacter) {
                R.string.practice_prompt_sign_to_character
            } else {
                R.string.practice_prompt_character_to_sign
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    if (exercise.type == PracticeExerciseType.SignToCharacter) {
        BrailleCellView(
            cell = exercise.target.cell,
            showPointNumbers = showPointNumbers,
            contentDescription = stringResource(
                R.string.practice_displayed_cell_description,
                activePointsText(exercise.target),
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
        )
    } else {
        Text(
            text = exercise.target.printedCharacter.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PracticeAnswerOption(
    option: BrailleCharacter,
    type: PracticeExerciseType,
    selected: Boolean,
    result: AnswerResult?,
    showPointNumbers: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val statusColors = BrailuxTheme.statusColors
    val stateText = when (result) {
        AnswerResult.Correct -> stringResource(R.string.practice_option_correct)
        AnswerResult.Incorrect -> stringResource(R.string.practice_option_incorrect)
        null -> stringResource(
            if (selected) R.string.settings_state_selected else R.string.settings_state_not_selected,
        )
    }
    val optionDescription = if (type == PracticeExerciseType.SignToCharacter) {
        stringResource(R.string.practice_letter_option_description, option.printedCharacter.toString())
    } else {
        stringResource(R.string.practice_cell_option_description, activePointsText(option))
    }
    val containerColor = when (result) {
        AnswerResult.Correct -> statusColors.successContainer
        AnswerResult.Incorrect -> MaterialTheme.colorScheme.errorContainer
        null -> if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    }
    val contentColor = when (result) {
        AnswerResult.Correct -> statusColors.onSuccessContainer
        AnswerResult.Incorrect -> MaterialTheme.colorScheme.onErrorContainer
        null -> if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    }
    val borderColor = when (result) {
        AnswerResult.Correct -> statusColors.onSuccessContainer
        AnswerResult.Incorrect -> MaterialTheme.colorScheme.onErrorContainer
        null -> if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    }

    val horizontalPadding = if (isCompact) 8.dp else 14.dp
    val verticalPadding = if (isCompact) 8.dp else 5.dp
    val minTouchTarget = if (isCompact) 48.dp else 56.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minTouchTarget)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = optionDescription
                stateDescription = stateText
            },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(if (selected) 3.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (type == PracticeExerciseType.SignToCharacter) {
                Text(
                    text = option.printedCharacter.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                BrailleCellView(
                    cell = option.cell,
                    showPointNumbers = showPointNumbers,
                    modifier = Modifier.clearAndSetSemantics { },
                    isCompact = isCompact,
                )
            }
            if (result != null) {
                Text(
                    text = stateText,
                    modifier = Modifier.padding(top = if (isCompact) 4.dp else 6.dp),
                    style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PointNumberToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = stringResource(
        if (checked) R.string.settings_state_enabled else R.string.settings_state_disabled,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.practice_show_point_numbers),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun BraillePracticeSummary(
    level: PracticeLevel,
    summary: PracticeSessionSummary,
    onPracticeAgain: () -> Unit,
    customConfiguration: CustomPracticeConfiguration? = null,
    onChangeConfiguration: (() -> Unit)? = null,
    engagementReward: EngagementReward? = null,
    onBackToPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val practicedContentNames = mutableListOf<String>()
    for (group in summary.practicedContentGroups) {
        practicedContentNames += stringResource(group.nameResource())
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (level == PracticeLevel.Daily) {
                DailyPracticeSummaryHeader()
            } else if (level == PracticeLevel.DailyChallenge) {
                DailyChallengeSummaryHeader()
            } else {
                BrailuxScreenHeader(
                    title = stringResource(level.completionTitleResource()),
                    subtitle = stringResource(level.titleResource()),
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                SummaryLine(
                    stringResource(
                        when {
                            level == PracticeLevel.Daily -> R.string.daily_practice_summary_completed_fraction
                            level == PracticeLevel.DailyChallenge -> R.string.daily_challenge_summary_completed_fraction
                            level != PracticeLevel.BrailleExplorer -> R.string.practice_summary_exercises_done
                            else -> R.string.practice_summary_completed
                        },
                        summary.exercisesCompleted,
                        summary.exercisesCompleted,
                    ),
                )
                SummaryLine(
                    stringResource(
                        R.string.practice_first_attempt_count,
                        summary.firstAttemptCorrect,
                    ),
                )
                SummaryLine(stringResource(R.string.practice_error_count, summary.errors))
                SummaryLine(
                    stringResource(
                        R.string.practice_summary_accuracy,
                        summary.accuracyPercentage,
                    ),
                )
                if (level == PracticeLevel.BrailleRecognizer || level == PracticeLevel.Custom) {
                    SummaryLine(
                        stringResource(R.string.practice_summary_hints_used, summary.hintsUsed),
                    )
                }
                if (level == PracticeLevel.Custom && customConfiguration != null) {
                    SummaryLine(
                        stringResource(
                            R.string.custom_practice_summary_content,
                            practicedContentNames.joinToString(", "),
                        ),
                    )
                    SummaryLine(
                        stringResource(
                            R.string.custom_practice_summary_mode,
                            stringResource(customConfiguration.mode.titleResource()),
                        ),
                    )
                } else if (level != PracticeLevel.Daily && level != PracticeLevel.DailyChallenge) {
                    SummaryLine(
                        stringResource(
                            R.string.practice_summary_letters,
                            summary.practicedLetters.joinToString(", "),
                        ),
                    )
                }
                if (engagementReward != null) {
                    XpRewardSummary(engagementReward.xpEarned)
                }
                if ((level == PracticeLevel.Daily || level == PracticeLevel.DailyChallenge) && engagementReward != null) {
                    val streakDays = pluralStringResource(
                        R.plurals.home_streak_days,
                        engagementReward.currentStreak,
                        engagementReward.currentStreak,
                    )
                    SummaryLine(
                        stringResource(
                            if (level == PracticeLevel.DailyChallenge) {
                                R.string.daily_challenge_reward_streak
                            } else {
                                R.string.daily_practice_reward_streak
                            },
                            streakDays,
                        ),
                    )
                    SummaryLine(
                        stringResource(
                            if (engagementReward.addedPracticeDay) {
                                R.string.daily_practice_reward_new_day
                            } else {
                                R.string.daily_practice_reward_day_already_counted
                            },
                        ),
                    )
                    SummaryLine(
                        stringResource(
                            R.string.daily_practice_reward_week,
                            engagementReward.weeklyPracticeDays,
                            com.brailuxaprende.practice.WeeklyPracticeTarget,
                        ),
                    )
                    engagementReward.miniAchievementCompleted?.let { mini ->
                        SummaryLine(
                            stringResource(
                                R.string.daily_practice_reward_mini,
                                stringResource(mini.titleResource()),
                            ),
                        )
                    }
                    val activeNewAchievements = engagementReward.newlyUnlockedAchievements.filterNot { it.isLegacy }
                    if (activeNewAchievements.isNotEmpty()) {
                        val count = activeNewAchievements.size
                        SummaryLine(
                            text = if (count == 1) {
                                stringResource(
                                    R.string.reward_new_badge_accessibility,
                                    stringResource(activeNewAchievements.first().titleResource()),
                                )
                            } else {
                                stringResource(R.string.reward_new_badge_plural, count)
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (count > 1) {
                            activeNewAchievements.forEach { achievement ->
                                SummaryLine(
                                    text = "• " + stringResource(achievement.titleResource()),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            if (level == PracticeLevel.Daily || level == PracticeLevel.DailyChallenge) {
                BrailuxPrimaryButton(
                    text = stringResource(
                        if (level == PracticeLevel.DailyChallenge) {
                            R.string.daily_challenge_back_to_home
                        } else {
                            R.string.daily_practice_back_to_home
                        },
                    ),
                    onClick = onBackToPractice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            } else {
                BrailuxPrimaryButton(
                    text = stringResource(R.string.practice_again),
                    onClick = onPracticeAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
                if (onChangeConfiguration != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    BrailuxSecondaryButton(
                        text = stringResource(R.string.custom_practice_change_configuration),
                        onClick = onChangeConfiguration,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                BrailuxSecondaryButton(
                    text = stringResource(R.string.practice_back_to_practice),
                    onClick = onBackToPractice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
        }
    }
}

@Composable
private fun XpRewardSummary(xpEarned: Int) {
    val accessibilityDescription = stringResource(
        R.string.practice_reward_xp_accessibility,
        xpEarned,
    )
    Text(
        text = stringResource(R.string.practice_reward_xp, xpEarned),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics {
                contentDescription = accessibilityDescription
            },
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SummaryLine(text: String, fontWeight: FontWeight = FontWeight.Normal) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = fontWeight,
    )
}

@Composable
private fun hintText(hint: PracticeHint): String = when (hint) {
    is PracticeHint.ActivePointCount -> stringResource(
        if (hint.count == 1) {
            R.string.practice_hint_point_count_one
        } else {
            R.string.practice_hint_point_count_many
        },
        hint.count,
    )
    is PracticeHint.ColumnDistribution -> stringResource(
        when {
            hint.leftCount > 0 && hint.rightCount > 0 -> R.string.practice_hint_both_columns
            hint.leftCount > 0 -> R.string.practice_hint_left_column
            hint.rightCount > 0 -> R.string.practice_hint_right_column
            else -> R.string.practice_hint_no_active_points
        },
    )
    is PracticeHint.RowState -> {
        val rowName = stringResource(
            when (hint.row) {
                BrailleRow.Top -> R.string.practice_hint_row_top
                BrailleRow.Middle -> R.string.practice_hint_row_middle
                BrailleRow.Bottom -> R.string.practice_hint_row_bottom
            },
        )
        stringResource(
            when (hint.activeCount) {
                0 -> R.string.practice_hint_row_empty
                1 -> R.string.practice_hint_row_one_point
                else -> R.string.practice_hint_row_two_points
            },
            rowName,
        )
    }
    is PracticeHint.PointState -> stringResource(
        if (hint.isActive) {
            R.string.practice_hint_point_active
        } else {
            R.string.practice_hint_point_inactive
        },
        hint.point,
    )
    is PracticeHint.CharacterCategory -> stringResource(
        if (hint.isVowel) {
            R.string.practice_hint_character_vowel
        } else {
            R.string.practice_hint_character_consonant
        },
    )
    is PracticeHint.AlphabetRange -> stringResource(
        R.string.practice_hint_alphabet_range,
        hint.first.toString(),
        hint.last.toString(),
    )
    is PracticeHint.AlphabetComparison -> stringResource(
        if (hint.targetComesAfter) {
            R.string.practice_hint_after_character
        } else {
            R.string.practice_hint_before_character
        },
        hint.reference.toString(),
    )
}

private fun activePointsText(character: BrailleCharacter): String =
    character.cell.activePoints().joinToString(", ")

@androidx.annotation.StringRes
private fun PracticeLevel.titleResource(): Int = when (this) {
    PracticeLevel.Daily -> R.string.home_daily_practice
    PracticeLevel.DailyChallenge -> R.string.home_daily_challenge
    PracticeLevel.BrailleExplorer -> R.string.practice_level_1_title
    PracticeLevel.BrailleRecognizer -> R.string.practice_level_2_title
    PracticeLevel.BrailleChallenge -> R.string.practice_level_3_title
    PracticeLevel.Custom -> R.string.practice_level_4_title
}

@androidx.annotation.StringRes
private fun PracticeLevel.completionTitleResource(): Int = when (this) {
    PracticeLevel.Daily -> R.string.daily_practice_completed
    PracticeLevel.DailyChallenge -> R.string.daily_challenge_completed
    PracticeLevel.BrailleChallenge -> R.string.practice_challenge_completed
    PracticeLevel.Custom -> R.string.custom_practice_completed
    PracticeLevel.BrailleExplorer,
    PracticeLevel.BrailleRecognizer -> R.string.practice_level_completed
}

@androidx.annotation.StringRes
private fun com.brailuxaprende.practice.DailyMiniAchievement.titleResource(): Int = when (this) {
    com.brailuxaprende.practice.DailyMiniAchievement.CompleteFiveExercises ->
        R.string.mini_achievement_five_exercises
    com.brailuxaprende.practice.DailyMiniAchievement.CompleteSession ->
        R.string.mini_achievement_session
    com.brailuxaprende.practice.DailyMiniAchievement.ThreeFirstAttemptCorrect ->
        R.string.mini_achievement_three_correct
    com.brailuxaprende.practice.DailyMiniAchievement.TwoModalities ->
        R.string.mini_achievement_two_modalities
}

@androidx.annotation.StringRes
private fun PracticeContentGroup.nameResource(): Int = when (this) {
    PracticeContentGroup.SpanishAlphabet -> R.string.custom_practice_alphabet
    PracticeContentGroup.AccentuationAndDiaeresis -> R.string.custom_practice_accents
    PracticeContentGroup.Punctuation -> R.string.custom_practice_punctuation
    PracticeContentGroup.Numbers -> R.string.custom_practice_numbers
    PracticeContentGroup.Capitals -> R.string.custom_practice_capitals
}

@androidx.annotation.StringRes
private fun PracticeMode.titleResource(): Int = when (this) {
    PracticeMode.SignToCharacter -> R.string.practice_mode_sign_to_character
    PracticeMode.CharacterToSign -> R.string.practice_mode_character_to_sign
    PracticeMode.Mixed -> R.string.practice_mode_mixed
}

@Composable
private fun DailyPracticeHeader(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onBack != null) {
            BrailuxBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(modifier = Modifier.heightIn(min = 20.dp))
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.daily_practice_header_title),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            DailyPracticeBadge(
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Text(
            text = stringResource(R.string.daily_practice_header_subtitle),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.daily_practice_habit_message),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DailyPracticeSummaryHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.daily_practice_completed),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            DailyPracticeBadge(
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Text(
            text = stringResource(R.string.daily_practice_completed_confirmation),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PracticeIdentityBadge(
    text: String,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .wrapContentWidth()
            .semantics {
                contentDescription = accessibilityDescription
            },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun DailyPracticeBadge(
    modifier: Modifier = Modifier,
) {
    PracticeIdentityBadge(
        text = stringResource(R.string.daily_practice_badge),
        accessibilityDescription = stringResource(R.string.daily_practice_badge_accessibility),
        modifier = modifier,
    )
}

@Composable
private fun DailyChallengeHeader(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onBack != null) {
            BrailuxBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(modifier = Modifier.heightIn(min = 20.dp))
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.daily_challenge_header_title),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            DailyChallengeBadge(
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Text(
            text = stringResource(R.string.daily_challenge_header_subtitle),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.daily_challenge_habit_message),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DailyChallengeSummaryHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.daily_challenge_completed),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            DailyChallengeBadge(
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Text(
            text = stringResource(R.string.daily_challenge_completed_confirmation),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DailyChallengeBadge(
    modifier: Modifier = Modifier,
) {
    PracticeIdentityBadge(
        text = stringResource(R.string.daily_challenge_badge),
        accessibilityDescription = stringResource(R.string.daily_challenge_badge_accessibility),
        modifier = modifier,
    )
}

