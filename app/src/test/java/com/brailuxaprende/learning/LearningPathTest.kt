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
    fun `all alphabet lessons are available or completed`() {
        assertEquals(
            List(5) { LearningLessonStatus.Available },
            LearningPath.lessons.map { LearningPath.statusFor(it, emptySet()) },
        )
        assertEquals(
            List(5) { LearningLessonStatus.Completed },
            LearningPath.lessons.map {
                LearningPath.statusFor(it, LearningPath.lessons.toSet())
            },
        )
    }
}
