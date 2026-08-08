package com.brailuxaprende.learning

import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.braille.BrailleRepository

enum class LearningLesson(val number: Int) {
    SixDots(number = 1),
    Vowels(number = 2),
    LettersAtoJ(number = 3),
    LettersKtoT(number = 4),
    LettersUtoZAndEnye(number = 5),
}

enum class LearningLessonStatus {
    Available,
    Completed,
}

object LearningPath {
    val lessons: List<LearningLesson> = LearningLesson.entries.sortedBy(LearningLesson::number)

    val vowels: List<BrailleCharacter>
        get() = BrailleRepository.getVowels()

    val lettersAtoJ: List<BrailleCharacter>
        get() = BrailleRepository.getLevel1Characters()

    val lettersKtoT: List<BrailleCharacter>
        get() = BrailleRepository.getLettersKToT()

    val lettersUtoZAndEnye: List<BrailleCharacter>
        get() = BrailleRepository.getLettersUToZAndEnye()

    fun nextLesson(lesson: LearningLesson): LearningLesson? {
        val index = lessons.indexOf(lesson)
        return lessons.getOrNull(index + 1)
    }

    fun statusFor(
        lesson: LearningLesson,
        completedLessons: Set<LearningLesson>,
    ): LearningLessonStatus = when {
        lesson in completedLessons -> LearningLessonStatus.Completed
        else -> LearningLessonStatus.Available
    }
}
