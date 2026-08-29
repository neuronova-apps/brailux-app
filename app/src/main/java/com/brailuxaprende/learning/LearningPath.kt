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
    Locked,
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

    fun previousLesson(lesson: LearningLesson): LearningLesson? {
        val index = lessons.indexOf(lesson)
        return if (index > 0) lessons[index - 1] else null
    }

    fun isUnlocked(
        lesson: LearningLesson,
        completedLessons: Set<LearningLesson>,
    ): Boolean = when (lesson) {
        LearningLesson.SixDots -> true
        LearningLesson.Vowels -> LearningLesson.SixDots in completedLessons
        LearningLesson.LettersAtoJ -> LearningLesson.Vowels in completedLessons
        LearningLesson.LettersKtoT -> LearningLesson.LettersAtoJ in completedLessons
        LearningLesson.LettersUtoZAndEnye -> LearningLesson.LettersKtoT in completedLessons
    }

    fun statusFor(
        lesson: LearningLesson,
        completedLessons: Set<LearningLesson>,
    ): LearningLessonStatus = when {
        lesson in completedLessons -> LearningLessonStatus.Completed
        isUnlocked(lesson, completedLessons) -> LearningLessonStatus.Available
        else -> LearningLessonStatus.Locked
    }

    fun completedCount(completedLessons: Set<LearningLesson>): Int =
        completedLessons.intersect(lessons.toSet()).size

    fun progressPercentage(completedLessons: Set<LearningLesson>): Int {
        if (lessons.isEmpty()) return 0
        return (completedCount(completedLessons) * 100) / lessons.size
    }
}
