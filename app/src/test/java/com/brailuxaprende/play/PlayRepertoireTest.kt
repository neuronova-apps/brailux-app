package com.brailuxaprende.play

import com.brailuxaprende.braille.BrailleRepository
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.learning.LearningLesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayRepertoireTest {

    @Test
    fun `before Vowels is completed no game is unlocked and repertoire is empty`() {
        val emptyProgress = LearningProgress(completedLessons = emptySet())
        val sixDotsProgress = LearningProgress(completedLessons = setOf(LearningLesson.SixDots))

        assertFalse(PlayRepertoireProvider.isGameUnlocked(PlayGame.Memory, emptyProgress))
        assertFalse(PlayRepertoireProvider.isGameUnlocked(PlayGame.Sequence, emptyProgress))
        assertFalse(PlayRepertoireProvider.isGameUnlocked(PlayGame.Order, emptyProgress))
        assertTrue(PlayRepertoireProvider.getAvailableRepertoire(emptyProgress).isEmpty())

        assertFalse(PlayRepertoireProvider.isGameUnlocked(PlayGame.Memory, sixDotsProgress))
        assertFalse(PlayRepertoireProvider.isGameUnlocked(PlayGame.Sequence, sixDotsProgress))
        assertFalse(PlayRepertoireProvider.isGameUnlocked(PlayGame.Order, sixDotsProgress))
        assertTrue(PlayRepertoireProvider.getAvailableRepertoire(sixDotsProgress).isEmpty())
    }

    @Test
    fun `when Vowels is completed only Memory is unlocked and repertoire has 5 vowels`() {
        val progress = LearningProgress(
            completedLessons = setOf(LearningLesson.SixDots, LearningLesson.Vowels),
        )

        assertTrue(PlayRepertoireProvider.isGameUnlocked(PlayGame.Memory, progress))
        assertFalse(PlayRepertoireProvider.isGameUnlocked(PlayGame.Sequence, progress))
        assertFalse(PlayRepertoireProvider.isGameUnlocked(PlayGame.Order, progress))

        val repertoire = PlayRepertoireProvider.getAvailableRepertoire(progress)
        assertEquals(5, repertoire.size)
        assertEquals(listOf('A', 'E', 'I', 'O', 'U'), repertoire.map { it.printedCharacter })
    }

    @Test
    fun `when LettersAtoJ is completed Memory and Sequence are unlocked and repertoire has A to J`() {
        val progress = LearningProgress(
            completedLessons = setOf(
                LearningLesson.SixDots,
                LearningLesson.Vowels,
                LearningLesson.LettersAtoJ,
            ),
        )

        assertTrue(PlayRepertoireProvider.isGameUnlocked(PlayGame.Memory, progress))
        assertTrue(PlayRepertoireProvider.isGameUnlocked(PlayGame.Sequence, progress))
        assertFalse(PlayRepertoireProvider.isGameUnlocked(PlayGame.Order, progress))

        val repertoire = PlayRepertoireProvider.getAvailableRepertoire(progress)
        assertEquals(10, repertoire.size)
        assertEquals(('A'..'J').toList(), repertoire.map { it.printedCharacter })
    }

    @Test
    fun `when LettersKtoT is completed all 3 games are unlocked and repertoire has A to T`() {
        val progress = LearningProgress(
            completedLessons = setOf(
                LearningLesson.SixDots,
                LearningLesson.Vowels,
                LearningLesson.LettersAtoJ,
                LearningLesson.LettersKtoT,
            ),
        )

        assertTrue(PlayRepertoireProvider.isGameUnlocked(PlayGame.Memory, progress))
        assertTrue(PlayRepertoireProvider.isGameUnlocked(PlayGame.Sequence, progress))
        assertTrue(PlayRepertoireProvider.isGameUnlocked(PlayGame.Order, progress))

        val repertoire = PlayRepertoireProvider.getAvailableRepertoire(progress)
        assertEquals(20, repertoire.size)
        assertEquals(('A'..'T').toList(), repertoire.map { it.printedCharacter })
    }

    @Test
    fun `when LettersUtoZAndEnye is completed all 3 games are unlocked and repertoire has all 27 Spanish characters`() {
        val progress = LearningProgress(
            completedLessons = setOf(
                LearningLesson.SixDots,
                LearningLesson.Vowels,
                LearningLesson.LettersAtoJ,
                LearningLesson.LettersKtoT,
                LearningLesson.LettersUtoZAndEnye,
            ),
        )

        assertTrue(PlayRepertoireProvider.isGameUnlocked(PlayGame.Memory, progress))
        assertTrue(PlayRepertoireProvider.isGameUnlocked(PlayGame.Sequence, progress))
        assertTrue(PlayRepertoireProvider.isGameUnlocked(PlayGame.Order, progress))

        val repertoire = PlayRepertoireProvider.getAvailableRepertoire(progress)
        assertEquals(27, repertoire.size)

        val expectedChars = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toList()
        assertEquals(expectedChars, repertoire.map { it.printedCharacter })
    }

    @Test
    fun `Spanish alphabet comparator orders Enye strictly between N and O`() {
        val comparator = PlayRepertoireProvider.spanishCharComparator

        assertTrue(comparator.compare('N', 'Ñ') < 0)
        assertTrue(comparator.compare('Ñ', 'O') < 0)
        assertTrue(comparator.compare('A', 'Ñ') < 0)
        assertTrue(comparator.compare('Ñ', 'Z') < 0)
        assertEquals(0, comparator.compare('Ñ', 'Ñ'))

        val unsorted = listOf('O', 'Z', 'A', 'Ñ', 'B', 'N')
        val sorted = unsorted.sortedWith(comparator)
        assertEquals(listOf('A', 'B', 'N', 'Ñ', 'O', 'Z'), sorted)
    }

    @Test
    fun `game status and required lessons match expectations`() {
        assertEquals(LearningLesson.Vowels, PlayRepertoireProvider.requiredLessonFor(PlayGame.Memory))
        assertEquals(LearningLesson.LettersAtoJ, PlayRepertoireProvider.requiredLessonFor(PlayGame.Sequence))
        assertEquals(LearningLesson.LettersKtoT, PlayRepertoireProvider.requiredLessonFor(PlayGame.Order))

        val emptyProgress = LearningProgress()
        assertEquals(PlayGameStatus.Locked, PlayRepertoireProvider.gameStatus(PlayGame.Memory, emptyProgress))
        assertEquals(PlayGameStatus.Locked, PlayRepertoireProvider.gameStatus(PlayGame.Sequence, emptyProgress))
        assertEquals(PlayGameStatus.Locked, PlayRepertoireProvider.gameStatus(PlayGame.Order, emptyProgress))

        val vowelsProgress = LearningProgress(completedLessons = setOf(LearningLesson.Vowels))
        assertEquals(PlayGameStatus.Available, PlayRepertoireProvider.gameStatus(PlayGame.Memory, vowelsProgress))
        assertEquals(PlayGameStatus.Locked, PlayRepertoireProvider.gameStatus(PlayGame.Sequence, vowelsProgress))
        assertEquals(PlayGameStatus.Locked, PlayRepertoireProvider.gameStatus(PlayGame.Order, vowelsProgress))
    }
}
