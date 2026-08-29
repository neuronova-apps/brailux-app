package com.brailuxaprende.practice

import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.learning.LearningLesson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrecisionAchievementEligibilityTest {

    @Test
    fun level1ExplorerIsNeverEligibleForPrecisionAchievements() {
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.BrailleExplorer,
                mode = PracticeMode.SignToCharacter,
            ),
        )
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.BrailleExplorer,
                mode = PracticeMode.CharacterToSign,
            ),
        )
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.BrailleExplorer,
                mode = PracticeMode.Mixed,
            ),
        )
    }

    @Test
    fun level2RecognizerIsEligibleOnlyInMixedMode() {
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.BrailleRecognizer,
                mode = PracticeMode.SignToCharacter,
            ),
        )
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.BrailleRecognizer,
                mode = PracticeMode.CharacterToSign,
            ),
        )
        assertTrue(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.BrailleRecognizer,
                mode = PracticeMode.Mixed,
            ),
        )
    }

    @Test
    fun level3ChallengeIsEligibleOnlyInMixedMode() {
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.BrailleChallenge,
                mode = PracticeMode.SignToCharacter,
            ),
        )
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.BrailleChallenge,
                mode = PracticeMode.CharacterToSign,
            ),
        )
        assertTrue(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.BrailleChallenge,
                mode = PracticeMode.Mixed,
            ),
        )
    }

    @Test
    fun dailyPracticeIsNeverEligibleForPrecisionAchievements() {
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.Daily,
                mode = PracticeMode.Mixed,
            ),
        )
    }

    @Test
    fun dailyChallengeIsEligibleOnlyAfterCompletingLettersAtoJInLearn() {
        val withoutLearnProgress = LearningProgress()
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.DailyChallenge,
                mode = PracticeMode.Mixed,
                learningProgress = withoutLearnProgress,
            ),
        )

        val withLearnProgress = LearningProgress(
            completedLessons = setOf(LearningLesson.LettersAtoJ),
        )
        assertTrue(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.DailyChallenge,
                mode = PracticeMode.Mixed,
                learningProgress = withLearnProgress,
            ),
        )
    }

    @Test
    fun customPracticeIsEligibleOnlyWithFullAlphabetMixedNoHintsAndAtLeast15Exercises() {
        val eligible15 = CustomPracticeConfiguration(
            exerciseCount = CustomExerciseCount.Fifteen,
            mode = PracticeMode.Mixed,
            hintsEnabled = false,
        )
        assertTrue(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.Custom,
                mode = PracticeMode.Mixed,
                customConfiguration = eligible15,
            ),
        )

        val eligible20 = eligible15.copy(exerciseCount = CustomExerciseCount.Twenty)
        assertTrue(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.Custom,
                mode = PracticeMode.Mixed,
                customConfiguration = eligible20,
            ),
        )

        val tooFewExercises = eligible15.copy(exerciseCount = CustomExerciseCount.Ten)
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.Custom,
                mode = PracticeMode.Mixed,
                customConfiguration = tooFewExercises,
            ),
        )

        val hintsEnabled = eligible15.copy(hintsEnabled = true)
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.Custom,
                mode = PracticeMode.Mixed,
                customConfiguration = hintsEnabled,
            ),
        )

        val notMixed = eligible15.copy(mode = PracticeMode.SignToCharacter)
        assertFalse(
            PrecisionAchievementEligibility.isEligible(
                level = PracticeLevel.Custom,
                mode = PracticeMode.SignToCharacter,
                customConfiguration = notMixed,
            ),
        )
    }
}
