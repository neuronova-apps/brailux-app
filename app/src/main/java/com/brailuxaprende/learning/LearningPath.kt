package com.brailuxaprende.learning

import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.braille.BrailleRepository

enum class LearningLesson(
    val number: Int,
    val implemented: Boolean,
) {
    SixDots(number = 1, implemented = true),
    Vowels(number = 2, implemented = true),
    LettersAtoJ(number = 3, implemented = true),
    LettersKtoT(number = 4, implemented = false),
    LettersUtoZAndEnye(number = 5, implemented = false),
}

enum class LearningLessonStatus {
    Available,
    Completed,
    ComingSoon,
}

object LearningPath {
    val lessons: List<LearningLesson> = LearningLesson.entries.sortedBy(LearningLesson::number)

    val vowels: List<BrailleCharacter>
        get() = BrailleRepository.getVowels()

    val lettersAtoJ: List<BrailleCharacter>
        get() = BrailleRepository.getLevel1Characters()

    fun nextLesson(lesson: LearningLesson): LearningLesson? {
        val index = lessons.indexOf(lesson)
        return lessons.getOrNull(index + 1)
    }

    fun statusFor(
        lesson: LearningLesson,
        completedLessons: Set<LearningLesson>,
    ): LearningLessonStatus = when {
        !lesson.implemented -> LearningLessonStatus.ComingSoon
        lesson in completedLessons -> LearningLessonStatus.Completed
        else -> LearningLessonStatus.Available
    }
}
