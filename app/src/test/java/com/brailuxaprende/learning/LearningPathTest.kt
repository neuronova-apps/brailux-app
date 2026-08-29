package com.brailuxaprende.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LearningPathTest {
    @Test
    fun `lessons keep the pedagogical order`() {
        assertEquals(
            listOf(
                LearningLesson.SixDots,
                LearningLesson.Vowels,
                LearningLesson.LettersAtoJ,
                LearningLesson.LettersKtoT,
                LearningLesson.LettersUtoZAndEnye,
            ),
            LearningPath.lessons,
        )
    }

    @Test
    fun `vowels come from the Braille repository in the correct order`() {
        assertEquals(
            listOf('A', 'E', 'I', 'O', 'U'),
            LearningPath.vowels.map { it.printedCharacter },
        )
    }

    @Test
    fun `letters A to J come from the Braille repository in the correct order`() {
        assertEquals(
            ('A'..'J').toList(),
            LearningPath.lettersAtoJ.map { it.printedCharacter },
        )
    }

    @Test
    fun `letters K to T keep their guided lesson order`() {
        assertEquals(
            ('K'..'T').toList(),
            LearningPath.lettersKtoT.map { it.printedCharacter },
        )
    }

    @Test
    fun `letters U to Z and enye keep their guided lesson order`() {
        assertEquals(
            ('U'..'Z').toList() + 'Ñ',
            LearningPath.lettersUtoZAndEnye.map { it.printedCharacter },
        )
    }

    @Test
    fun `next lesson follows the guided path without loops`() {
        assertEquals(LearningLesson.Vowels, LearningPath.nextLesson(LearningLesson.SixDots))
        assertEquals(LearningLesson.LettersAtoJ, LearningPath.nextLesson(LearningLesson.Vowels))
        assertEquals(LearningLesson.LettersKtoT, LearningPath.nextLesson(LearningLesson.LettersAtoJ))
        assertEquals(
            LearningLesson.LettersUtoZAndEnye,
            LearningPath.nextLesson(LearningLesson.LettersKtoT),
        )
        assertNull(LearningPath.nextLesson(LearningLesson.LettersUtoZAndEnye))
    }

    @Test
    fun `progressive unlock opens lessons sequentially`() {
        // Initial state: only Lesson 1 (SixDots) is available, others are locked
        assertEquals(LearningLessonStatus.Available, LearningPath.statusFor(LearningLesson.SixDots, emptySet()))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.Vowels, emptySet()))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.LettersAtoJ, emptySet()))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.LettersKtoT, emptySet()))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.LettersUtoZAndEnye, emptySet()))

        // After completing Lesson 1: Lesson 1 is completed, Lesson 2 is available, 3..5 locked
        val progress1 = setOf(LearningLesson.SixDots)
        assertEquals(LearningLessonStatus.Completed, LearningPath.statusFor(LearningLesson.SixDots, progress1))
        assertEquals(LearningLessonStatus.Available, LearningPath.statusFor(LearningLesson.Vowels, progress1))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.LettersAtoJ, progress1))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.LettersKtoT, progress1))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.LettersUtoZAndEnye, progress1))

        // After completing Lesson 2: Lesson 1 & 2 completed, Lesson 3 available, 4..5 locked
        val progress2 = progress1 + LearningLesson.Vowels
        assertEquals(LearningLessonStatus.Completed, LearningPath.statusFor(LearningLesson.SixDots, progress2))
        assertEquals(LearningLessonStatus.Completed, LearningPath.statusFor(LearningLesson.Vowels, progress2))
        assertEquals(LearningLessonStatus.Available, LearningPath.statusFor(LearningLesson.LettersAtoJ, progress2))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.LettersKtoT, progress2))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.LettersUtoZAndEnye, progress2))

        // After completing Lesson 3: Lesson 4 available
        val progress3 = progress2 + LearningLesson.LettersAtoJ
        assertEquals(LearningLessonStatus.Available, LearningPath.statusFor(LearningLesson.LettersKtoT, progress3))
        assertEquals(LearningLessonStatus.Locked, LearningPath.statusFor(LearningLesson.LettersUtoZAndEnye, progress3))

        // After completing Lesson 4: Lesson 5 available
        val progress4 = progress3 + LearningLesson.LettersKtoT
        assertEquals(LearningLessonStatus.Available, LearningPath.statusFor(LearningLesson.LettersUtoZAndEnye, progress4))

        // After completing all lessons: all are completed
        val progress5 = progress4 + LearningLesson.LettersUtoZAndEnye
        assertEquals(
            List(5) { LearningLessonStatus.Completed },
            LearningPath.lessons.map { LearningPath.statusFor(it, progress5) },
        )
    }

    @Test
    fun `completed count and progress percentage calculate accurately`() {
        assertEquals(0, LearningPath.completedCount(emptySet()))
        assertEquals(0, LearningPath.progressPercentage(emptySet()))

        val one = setOf(LearningLesson.SixDots)
        assertEquals(1, LearningPath.completedCount(one))
        assertEquals(20, LearningPath.progressPercentage(one))

        val two = one + LearningLesson.Vowels
        assertEquals(2, LearningPath.completedCount(two))
        assertEquals(40, LearningPath.progressPercentage(two))

        val three = two + LearningLesson.LettersAtoJ
        assertEquals(3, LearningPath.completedCount(three))
        assertEquals(60, LearningPath.progressPercentage(three))

        val four = three + LearningLesson.LettersKtoT
        assertEquals(4, LearningPath.completedCount(four))
        assertEquals(80, LearningPath.progressPercentage(four))

        val all = four + LearningLesson.LettersUtoZAndEnye
        assertEquals(5, LearningPath.completedCount(all))
        assertEquals(100, LearningPath.progressPercentage(all))
    }
}
