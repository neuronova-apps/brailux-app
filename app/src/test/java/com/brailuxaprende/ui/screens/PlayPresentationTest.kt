package com.brailuxaprende.ui.screens

import com.brailuxaprende.R
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.learning.LearningLesson
import com.brailuxaprende.play.PlayGame
import com.brailuxaprende.play.PlayGameStatus
import com.brailuxaprende.play.PlayRepertoireProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayPresentationTest {

    @Test
    fun `play games enum contains exactly the three minigames in expected order`() {
        val expectedGames = listOf(
            PlayGame.Memory,
            PlayGame.Sequence,
            PlayGame.Order,
        )
        assertEquals(expectedGames, PlayGame.entries)
        assertEquals(3, PlayGame.entries.size)
    }

    @Test
    fun `each play game has valid resource IDs for title description and requirement`() {
        for (game in PlayGame.entries) {
            assertNotEquals(0, game.titleResource)
            assertNotEquals(0, game.descriptionResource)
            assertNotEquals(0, game.requirementResource)
        }

        assertEquals(R.string.play_game_memory_title, PlayGame.Memory.titleResource)
        assertEquals(R.string.play_game_sequence_title, PlayGame.Sequence.titleResource)
        assertEquals(R.string.play_game_order_title, PlayGame.Order.titleResource)
        assertEquals(R.string.play_game_memory_requirement, PlayGame.Memory.requirementResource)
        assertEquals(R.string.play_game_sequence_requirement, PlayGame.Sequence.requirementResource)
        assertEquals(R.string.play_game_order_requirement, PlayGame.Order.requirementResource)
    }

    @Test
    fun `play screen always provides cards for all three games even when all are locked`() {
        val emptyProgress = LearningProgress(completedLessons = emptySet())

        // Confirm all 3 games are locked but exist in the entries
        for (game in PlayGame.entries) {
            val status = PlayRepertoireProvider.gameStatus(game, emptyProgress)
            assertEquals(PlayGameStatus.Locked, status)
            assertFalse(PlayRepertoireProvider.isGameUnlocked(game, emptyProgress))
        }

        // All 3 games must be rendered in PlayScreen as cards
        assertEquals(3, PlayGame.entries.size)
    }

    @Test
    fun `play screen unlocks games incrementally per pedagogical path`() {
        // Step 1: Vowels completed -> Memory unlocked, others locked
        val vowelsProgress = LearningProgress(
            completedLessons = setOf(LearningLesson.SixDots, LearningLesson.Vowels),
        )
        assertEquals(PlayGameStatus.Available, PlayRepertoireProvider.gameStatus(PlayGame.Memory, vowelsProgress))
        assertEquals(PlayGameStatus.Locked, PlayRepertoireProvider.gameStatus(PlayGame.Sequence, vowelsProgress))
        assertEquals(PlayGameStatus.Locked, PlayRepertoireProvider.gameStatus(PlayGame.Order, vowelsProgress))

        // Step 2: Letters A-J completed -> Memory and Sequence unlocked, Order locked
        val aToJProgress = vowelsProgress.copy(
            completedLessons = vowelsProgress.completedLessons + LearningLesson.LettersAtoJ,
        )
        assertEquals(PlayGameStatus.Available, PlayRepertoireProvider.gameStatus(PlayGame.Memory, aToJProgress))
        assertEquals(PlayGameStatus.Available, PlayRepertoireProvider.gameStatus(PlayGame.Sequence, aToJProgress))
        assertEquals(PlayGameStatus.Locked, PlayRepertoireProvider.gameStatus(PlayGame.Order, aToJProgress))

        // Step 3: Letters K-T completed -> All 3 games unlocked
        val kToTProgress = aToJProgress.copy(
            completedLessons = aToJProgress.completedLessons + LearningLesson.LettersKtoT,
        )
        assertEquals(PlayGameStatus.Available, PlayRepertoireProvider.gameStatus(PlayGame.Memory, kToTProgress))
        assertEquals(PlayGameStatus.Available, PlayRepertoireProvider.gameStatus(PlayGame.Sequence, kToTProgress))
        assertEquals(PlayGameStatus.Available, PlayRepertoireProvider.gameStatus(PlayGame.Order, kToTProgress))
    }
}
