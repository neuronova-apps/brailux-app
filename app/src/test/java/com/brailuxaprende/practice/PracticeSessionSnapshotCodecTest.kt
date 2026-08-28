package com.brailuxaprende.practice

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionSnapshotCodecTest {
    @Test
    fun sessionRoundTripsAfterCompletingExerciseOne() {
        val state = progressedState(
            PracticeSessionGenerator.generate(random = Random(13)),
            completed = 1,
        )

        val restored = roundTrip(PracticeSessionSnapshot(state = state))

        assertEquals(1, restored.state.currentExerciseIndex)
        assertEquals(1, restored.state.completedAnswers.size)
        assertEquals(state, restored.state)
    }

    @Test
    fun dailySessionRoundTripsAtMidpoint() {
        assertActiveRoundTrip(
            progressedState(PracticeSessionGenerator.generateDaily(Random(1)), completed = 2),
        )
    }

    @Test
    fun level1SessionRoundTripsAtMidpoint() {
        assertActiveRoundTrip(
            progressedState(PracticeSessionGenerator.generate(random = Random(2)), completed = 4),
        )
    }

    @Test
    fun level2SessionRoundTripsAtMidpointWithHintsAndCurrentResponses() {
        var state = progressedState(
            PracticeSessionGenerator.generateLevel2(PracticeMode.Mixed, Random(3)),
            completed = 7,
        )
        state = state.showHint()
        state = answerIncorrectly(state)

        val restored = roundTrip(PracticeSessionSnapshot(state = state))

        assertEquals(state, restored.state)
        assertEquals(1, restored.state.currentExerciseAnswers.size)
        assertEquals(1, restored.state.errors)
        assertEquals(1, restored.state.hintsUsed)
    }

    @Test
    fun level3SessionRoundTripsAtMidpoint() {
        assertActiveRoundTrip(
            progressedState(
                PracticeSessionGenerator.generateLevel3(PracticeMode.Mixed, Random(4)),
                completed = 10,
            ),
        )
    }

    @Test
    fun customSessionRoundTripsWithItsCompleteConfiguration() {
        val configuration = CustomPracticeConfiguration(
            additionalContentGroups = emptySet(),
            exerciseCount = CustomExerciseCount.Twenty,
            mode = PracticeMode.Mixed,
            hintsEnabled = false,
            showPointNumbers = false,
        )
        val state = progressedState(
            PracticeSessionGenerator.generateCustom(configuration, Random(5)),
            completed = 9,
        )

        val restored = roundTrip(PracticeSessionSnapshot(state = state))

        assertEquals(configuration, restored.state.session.customConfiguration)
        assertEquals(state.session.exercises, restored.state.session.exercises)
        assertEquals(state, restored.state)
    }

    @Test
    fun lastCorrectExerciseRoundTripsWithoutCompletingEarly() {
        val session = PracticeSessionGenerator.generateLevel3(PracticeMode.Mixed, Random(6))
        var state = progressedState(session, completed = session.exercises.lastIndex)
        state = answerCorrectly(state)

        val restored = roundTrip(PracticeSessionSnapshot(state = state))

        assertEquals(session.exercises.lastIndex, restored.state.currentExerciseIndex)
        assertEquals(PracticeValidationState.Correct, restored.state.validation)
        assertTrue(!restored.state.isCompleted)
        assertEquals(state, restored.state)
    }

    @Test
    fun repeatingFromSummaryCreatesANewLegitimateSessionId() {
        val completed = complete(
            PracticeSessionState(
                PracticeSessionGenerator.generate(random = Random(14)),
                sessionId = "completed-session",
            ),
        )
        val summary = PracticeSessionSnapshot(
            state = completed,
            phase = PracticeSessionPhase.Credited,
        )
        val repeated = PracticeSessionSnapshot(
            state = PracticeSessionState(
                PracticeSessionGenerator.generate(random = Random(15)),
            ),
        )

        assertEquals("completed-session", summary.sessionId)
        assertNotEquals(summary.sessionId, repeated.sessionId)
        assertEquals(PracticeSessionPhase.Active, repeated.phase)
    }

    @Test
    fun staleCreditCallbackCannotOverwriteARepeatedClearedOrResolvedSession() {
        val completed = complete(
            PracticeSessionState(
                PracticeSessionGenerator.generate(random = Random(16)),
                sessionId = "pending-credit-session",
            ),
        )
        val pending = PracticeSessionSnapshot(
            state = completed,
            phase = PracticeSessionPhase.AwaitingCredit,
            creditAttempt = 1,
        )
        val credited = pending.copy(phase = PracticeSessionPhase.Credited)
        val retried = pending.copy(
            phase = PracticeSessionPhase.AwaitingCredit,
            creditAttempt = 2,
        )
        val repeated = PracticeSessionSnapshot(
            state = PracticeSessionState(
                PracticeSessionGenerator.generate(random = Random(17)),
            ),
        )

        assertTrue(pending.acceptsCreditResolution(credited))
        assertFalse(retried.acceptsCreditResolution(credited))
        assertTrue(
            retried.acceptsCreditResolution(
                retried.copy(phase = PracticeSessionPhase.Credited),
            ),
        )
        assertFalse(credited.acceptsCreditResolution(credited))
        assertFalse(repeated.acceptsCreditResolution(credited))
        assertFalse((null as PracticeSessionSnapshot?).acceptsCreditResolution(credited))
    }

    @Test
    fun completedSummaryAndRewardRoundTrip() {
        val completed = complete(
            PracticeSessionState(
                session = PracticeSessionGenerator.generateDaily(Random(7)),
                sessionId = "daily-summary-session",
            ),
        )
        val reward = EngagementReward(
            xpEarned = 30,
            addedPracticeDay = true,
            weeklyPracticeDays = 4,
            currentStreak = 3,
            miniAchievementCompleted = DailyMiniAchievement.CompleteFiveExercises,
            newlyUnlockedAchievements = setOf(
                PermanentAchievement.FirstStep,
                PermanentAchievement.Consistency,
            ),
        )
        val snapshot = PracticeSessionSnapshot(
            state = completed,
            phase = PracticeSessionPhase.Credited,
            engagementReward = reward,
        )

        val restored = roundTrip(snapshot)

        assertEquals(snapshot, restored)
        assertEquals("daily-summary-session", restored.summary?.sessionId)
        assertEquals(completed.summary(), restored.summary)
    }

    @Test
    fun awaitingCreditAndCreditFailedPhasesRoundTrip() {
        val completed = complete(
            PracticeSessionState(PracticeSessionGenerator.generate(random = Random(8))),
        )
        listOf(PracticeSessionPhase.AwaitingCredit, PracticeSessionPhase.CreditFailed).forEach { phase ->
            val snapshot = PracticeSessionSnapshot(
                state = completed,
                phase = phase,
                creditAttempt = 3,
            )
            assertEquals(snapshot, roundTrip(snapshot))
        }
    }

    @Test
    fun corruptTruncatedUnknownAndUnsupportedPayloadsAreRejected() {
        val encoded = PracticeSessionSnapshotCodec.encode(
            PracticeSessionSnapshot(
                state = PracticeSessionState(PracticeSessionGenerator.generate(random = Random(9))),
            ),
        )
        val unsupportedVersion = encoded.substring(0, 8) + "00000002" + encoded.substring(16)

        assertNull(PracticeSessionSnapshotCodec.decode("not-hex"))
        assertNull(PracticeSessionSnapshotCodec.decode(encoded.dropLast(2)))
        assertNull(PracticeSessionSnapshotCodec.decode(unsupportedVersion))
    }

    @Test
    fun level1SnapshotPreservesModeAndIdentifiesIncompatibleMode() {
        val characterToSignState = PracticeSessionState(
            PracticeSessionGenerator.generate(PracticeMode.CharacterToSign, Random(31)),
        )
        val snapshot = roundTrip(PracticeSessionSnapshot(state = characterToSignState))

        assertEquals(PracticeLevel.BrailleExplorer, snapshot.level)
        assertEquals(PracticeMode.CharacterToSign, snapshot.state.session.mode)
        assertTrue(snapshot.state.session.exercises.all { it.type == PracticeExerciseType.CharacterToSign })
        assertNotEquals(PracticeMode.SignToCharacter, snapshot.state.session.mode)
    }

    @Test
    fun level2SnapshotPreservesModeAndIdentifiesIncompatibleMode() {
        val mixedState = PracticeSessionState(
            PracticeSessionGenerator.generateLevel2(PracticeMode.Mixed, Random(32)),
        )
        val snapshot = roundTrip(PracticeSessionSnapshot(state = mixedState))

        assertEquals(PracticeLevel.BrailleRecognizer, snapshot.level)
        assertEquals(PracticeMode.Mixed, snapshot.state.session.mode)
        assertEquals(PracticeExerciseType.entries.toSet(), snapshot.state.session.exercises.map { it.type }.toSet())
        assertNotEquals(PracticeMode.SignToCharacter, snapshot.state.session.mode)
    }

    @Test
    fun level3SnapshotPreservesModeAndIdentifiesIncompatibleMode() {
        val signToCharState = PracticeSessionState(
            PracticeSessionGenerator.generateLevel3(PracticeMode.SignToCharacter, Random(33)),
        )
        val snapshot = roundTrip(PracticeSessionSnapshot(state = signToCharState))

        assertEquals(PracticeLevel.BrailleChallenge, snapshot.level)
        assertEquals(PracticeMode.SignToCharacter, snapshot.state.session.mode)
        assertTrue(snapshot.state.session.exercises.all { it.type == PracticeExerciseType.SignToCharacter })
        assertNotEquals(PracticeMode.CharacterToSign, snapshot.state.session.mode)
    }

    private fun assertActiveRoundTrip(state: PracticeSessionState) {
        val restored = roundTrip(PracticeSessionSnapshot(state = state))
        assertEquals(state, restored.state)
        assertEquals(state.session.exercises, restored.state.session.exercises)
        assertEquals(state.sessionId, restored.sessionId)
    }

    private fun roundTrip(snapshot: PracticeSessionSnapshot): PracticeSessionSnapshot {
        val encoded = PracticeSessionSnapshotCodec.encode(snapshot)
        return requireNotNull(PracticeSessionSnapshotCodec.decode(encoded))
    }

    private fun progressedState(
        session: PracticeSession,
        completed: Int,
    ): PracticeSessionState {
        var state = PracticeSessionState(session = session, sessionId = "${session.level}-$completed")
        repeat(completed) { state = answerCorrectly(state).nextExercise() }
        return state
    }

    private fun complete(initial: PracticeSessionState): PracticeSessionState {
        var state = initial
        while (!state.isCompleted) {
            if (state.validation != PracticeValidationState.Correct) state = answerCorrectly(state)
            state = state.nextExercise()
        }
        return state
    }

    private fun answerCorrectly(state: PracticeSessionState): PracticeSessionState = state
        .selectAnswer(state.currentExercise.target.printedCharacter)
        .checkAnswer()

    private fun answerIncorrectly(state: PracticeSessionState): PracticeSessionState {
        val incorrect = state.currentExercise.options.first {
            it.printedCharacter != state.currentExercise.target.printedCharacter
        }
        return state.selectAnswer(incorrect.printedCharacter).checkAnswer()
    }
}
