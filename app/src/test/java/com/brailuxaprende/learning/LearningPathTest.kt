package com.brailuxaprende.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `next lesson follows the guided path without loops`() {
        assertEquals(LearningLesson.Vowels, LearningPath.nextLesson(LearningLesson.SixDots))
        assertEquals(LearningLesson.LettersAtoJ, LearningPath.nextLesson(LearningLesson.Vowels))
        assertNull(LearningPath.nextLesson(LearningLesson.LettersUtoZAndEnye))
    }

    @Test
    fun `future lessons remain explicitly unavailable`() {
        assertTrue(LearningLesson.SixDots.implemented)
        assertTrue(LearningLesson.Vowels.implemented)
        assertTrue(LearningLesson.LettersAtoJ.implemented)
        assertFalse(LearningLesson.LettersKtoT.implemented)
        assertFalse(LearningLesson.LettersUtoZAndEnye.implemented)
    }
}
