package com.brailuxaprende.learning

import com.brailuxaprende.braille.BrailleCell
import com.brailuxaprende.braille.BrailleRepository
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.practice.PracticeLevel
import com.brailuxaprende.practice.PracticeSessionGenerator
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.ui.screens.titleResource
import com.brailuxaprende.ui.screens.labelResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LearnModuleFeatureTest {

    @Test
    fun `there are exactly five main learning blocks in pedagogical order`() {
        assertEquals(5, LearningPath.lessons.size)
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
        assertEquals(1, LearningLesson.SixDots.number)
        assertEquals(2, LearningLesson.Vowels.number)
        assertEquals(3, LearningLesson.LettersAtoJ.number)
        assertEquals(4, LearningLesson.LettersKtoT.number)
        assertEquals(5, LearningLesson.LettersUtoZAndEnye.number)
    }

    @Test
    fun `block 1 six dots defines valid 1 to 6 cell distribution`() {
        val cellAllPoints = BrailleCell.fromPoints(setOf(1, 2, 3, 4, 5, 6))
        for (i in 1..6) {
            assertTrue("Point $i must be active in full cell", cellAllPoints.isPointActive(i))
        }
        assertEquals(listOf(1, 2, 3, 4, 5, 6), cellAllPoints.activePoints())
    }

    @Test
    fun `block 2 vowels contains exactly A E I O U with accurate Braille patterns`() {
        val vowels = LearningPath.vowels
        assertEquals(listOf('A', 'E', 'I', 'O', 'U'), vowels.map { it.printedCharacter })

        val a = requireNotNull(BrailleRepository.findCharacter('A'))
        assertEquals(listOf(1), a.cell.activePoints())

        val e = requireNotNull(BrailleRepository.findCharacter('E'))
        assertEquals(listOf(1, 5), e.cell.activePoints())

        val i = requireNotNull(BrailleRepository.findCharacter('I'))
        assertEquals(listOf(2, 4), i.cell.activePoints())

        val o = requireNotNull(BrailleRepository.findCharacter('O'))
        assertEquals(listOf(1, 3, 5), o.cell.activePoints())

        val u = requireNotNull(BrailleRepository.findCharacter('U'))
        assertEquals(listOf(1, 3, 6), u.cell.activePoints())
    }

    @Test
    fun `block 3 letters A to J contains exactly A through J`() {
        val lettersAtoJ = LearningPath.lettersAtoJ
        assertEquals(('A'..'J').toList(), lettersAtoJ.map { it.printedCharacter })
        assertEquals(10, lettersAtoJ.size)
    }

    @Test
    fun `block 4 letters K to T contains exactly K through T`() {
        val lettersKtoT = LearningPath.lettersKtoT
        assertEquals(('K'..'T').toList(), lettersKtoT.map { it.printedCharacter })
        assertEquals(10, lettersKtoT.size)
        // Ensure Ñ is not misplaced in K-T
        assertFalse(lettersKtoT.any { it.printedCharacter == 'Ñ' })
    }

    @Test
    fun `block 5 letters U to Z and enye contains exactly U through Z plus enye`() {
        val lettersUtoZAndEnye = LearningPath.lettersUtoZAndEnye
        val expected = ('U'..'Z').toList() + 'Ñ'
        assertEquals(expected, lettersUtoZAndEnye.map { it.printedCharacter })
        assertEquals(7, lettersUtoZAndEnye.size)

        val enye = requireNotNull(BrailleRepository.findCharacter('Ñ'))
        assertEquals(listOf(1, 2, 4, 5, 6), enye.cell.activePoints())
    }

    @Test
    fun `sequential unlocking progresses in strict order`() {
        val noProgress = emptySet<LearningLesson>()
        assertTrue(LearningPath.isUnlocked(LearningLesson.SixDots, noProgress))
        assertFalse(LearningPath.isUnlocked(LearningLesson.Vowels, noProgress))
        assertFalse(LearningPath.isUnlocked(LearningLesson.LettersAtoJ, noProgress))
        assertFalse(LearningPath.isUnlocked(LearningLesson.LettersKtoT, noProgress))
        assertFalse(LearningPath.isUnlocked(LearningLesson.LettersUtoZAndEnye, noProgress))

        val afterLesson1 = setOf(LearningLesson.SixDots)
        assertTrue(LearningPath.isUnlocked(LearningLesson.Vowels, afterLesson1))
        assertFalse(LearningPath.isUnlocked(LearningLesson.LettersAtoJ, afterLesson1))

        val afterLesson2 = afterLesson1 + LearningLesson.Vowels
        assertTrue(LearningPath.isUnlocked(LearningLesson.LettersAtoJ, afterLesson2))
        assertFalse(LearningPath.isUnlocked(LearningLesson.LettersKtoT, afterLesson2))

        val afterLesson3 = afterLesson2 + LearningLesson.LettersAtoJ
        assertTrue(LearningPath.isUnlocked(LearningLesson.LettersKtoT, afterLesson3))
        assertFalse(LearningPath.isUnlocked(LearningLesson.LettersUtoZAndEnye, afterLesson3))

        val afterLesson4 = afterLesson3 + LearningLesson.LettersKtoT
        assertTrue(LearningPath.isUnlocked(LearningLesson.LettersUtoZAndEnye, afterLesson4))
    }

    @Test
    fun `progress percentage accurately computes 0 to 100 percent across 5 blocks`() {
        assertEquals(0, LearningPath.progressPercentage(emptySet()))
        assertEquals(20, LearningPath.progressPercentage(setOf(LearningLesson.SixDots)))
        assertEquals(40, LearningPath.progressPercentage(setOf(LearningLesson.SixDots, LearningLesson.Vowels)))
        assertEquals(
            60,
            LearningPath.progressPercentage(
                setOf(LearningLesson.SixDots, LearningLesson.Vowels, LearningLesson.LettersAtoJ),
            ),
        )
        assertEquals(
            80,
            LearningPath.progressPercentage(
                setOf(
                    LearningLesson.SixDots,
                    LearningLesson.Vowels,
                    LearningLesson.LettersAtoJ,
                    LearningLesson.LettersKtoT,
                ),
            ),
        )
        assertEquals(
            100,
            LearningPath.progressPercentage(LearningLesson.entries.toSet()),
        )
    }

    @Test
    fun `daily practice and daily challenge adaptively query LearningProgress consistently`() {
        val initialProgress = LearningProgress(completedLessons = emptySet())
        val initialChars = PracticeSessionGenerator.availableCharactersForDaily(
            learningProgress = initialProgress,
            practiceProgress = PracticeProgress(),
        )
        assertEquals(('A'..'J').toList(), initialChars.map { it.printedCharacter })

        val progressLesson4 = LearningProgress(
            completedLessons = setOf(
                LearningLesson.SixDots,
                LearningLesson.Vowels,
                LearningLesson.LettersAtoJ,
                LearningLesson.LettersKtoT,
            ),
        )
        val charsLesson4 = PracticeSessionGenerator.availableCharactersForDaily(
            learningProgress = progressLesson4,
            practiceProgress = PracticeProgress(),
        )
        assertEquals(('A'..'T').toList(), charsLesson4.map { it.printedCharacter })

        val progressLesson5 = LearningProgress(
            completedLessons = LearningLesson.entries.toSet(),
        )
        val fullChars = PracticeSessionGenerator.availableCharactersForDaily(
            learningProgress = progressLesson5,
            practiceProgress = PracticeProgress(),
        )
        val expectedFull = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toList()
        assertEquals(expectedFull, fullChars.map { it.printedCharacter })

        // Daily practice generation maintains 5 exercises
        val dailySession = PracticeSessionGenerator.generateDaily(
            date = PracticeDate(2026, 8, 28),
            learningProgress = progressLesson4,
            practiceProgress = PracticeProgress(),
            random = Random(42),
        )
        assertEquals(5, dailySession.exercises.size)
        assertEquals(PracticeLevel.Daily, dailySession.level)

        // Daily challenge generation maintains 10 exercises
        val dailyChallengeSession = PracticeSessionGenerator.generateDailyChallenge(
            date = PracticeDate(2026, 8, 28),
            learningProgress = progressLesson5,
            practiceProgress = PracticeProgress(),
            random = Random(42),
        )
        assertEquals(10, dailyChallengeSession.exercises.size)
        assertEquals(PracticeLevel.DailyChallenge, dailyChallengeSession.level)
    }

    @Test
    fun `practice levels 1 to 4 remain intact`() {
        val level1 = PracticeSessionGenerator.generate(random = Random(1))
        assertEquals(PracticeLevel.BrailleExplorer, level1.level)
        assertEquals(10, level1.exercises.size)

        val level2 = PracticeSessionGenerator.generateLevel2(random = Random(1))
        assertEquals(PracticeLevel.BrailleRecognizer, level2.level)
        assertEquals(15, level2.exercises.size)

        val level3 = PracticeSessionGenerator.generateLevel3(random = Random(1))
        assertEquals(PracticeLevel.BrailleChallenge, level3.level)
        assertEquals(20, level3.exercises.size)
    }

    @Test
    fun `lesson and status titles map to existing resources`() {
        LearningLesson.entries.forEach { lesson ->
            val res = lesson.titleResource()
            assertTrue(res > 0)
        }
        LearningLessonStatus.entries.forEach { status ->
            val res = status.labelResource()
            assertTrue(res > 0)
        }
    }
}
