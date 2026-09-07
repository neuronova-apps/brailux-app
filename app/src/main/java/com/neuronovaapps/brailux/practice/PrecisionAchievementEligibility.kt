package com.neuronovaapps.brailux.practice

import com.neuronovaapps.brailux.data.learn.LearningProgress
import com.neuronovaapps.brailux.learning.LearningLesson

object PrecisionAchievementEligibility {
    fun isEligible(
        level: PracticeLevel,
        mode: PracticeMode,
        customConfiguration: CustomPracticeConfiguration? = null,
        learningProgress: LearningProgress = LearningProgress(),
    ): Boolean = when (level) {
        PracticeLevel.BrailleExplorer -> false
        PracticeLevel.BrailleRecognizer -> mode == PracticeMode.Mixed
        PracticeLevel.BrailleChallenge -> mode == PracticeMode.Mixed
        PracticeLevel.Daily -> false
        PracticeLevel.DailyChallenge -> learningProgress.isCompleted(LearningLesson.LettersAtoJ)
        PracticeLevel.Custom -> {
            val config = customConfiguration ?: return false
            config.exerciseCount.value >= 15 &&
                config.additionalContentGroups.isEmpty() &&
                config.mode == PracticeMode.Mixed &&
                !config.hintsEnabled
        }
    }
}
