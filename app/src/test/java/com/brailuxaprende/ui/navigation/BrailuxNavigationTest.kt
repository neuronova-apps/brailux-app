package com.brailuxaprende.ui.navigation

import com.brailuxaprende.learning.LearningLesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailuxNavigationTest {

    @Test
    fun `bottom navigation keeps the four main destinations in order`() {
        assertEquals(
            listOf(
                BrailuxRoutes.HOME,
                BrailuxRoutes.LEARN,
                BrailuxRoutes.PLAY,
                BrailuxRoutes.PROGRESS,
            ),
            bottomDestinationRoutes(),
        )
    }

    @Test
    fun `bottom navigation selects only the active main destination`() {
        assertEquals(BrailuxRoutes.HOME, selectedMainDestination(BrailuxRoutes.HOME))
        assertEquals(BrailuxRoutes.LEARN, selectedMainDestination(BrailuxRoutes.LEARN))
        assertEquals(BrailuxRoutes.PLAY, selectedMainDestination(BrailuxRoutes.PLAY))
        assertEquals(BrailuxRoutes.PROGRESS, selectedMainDestination(BrailuxRoutes.PROGRESS))
        assertNull(selectedMainDestination(BrailuxRoutes.PRACTICE))
    }

    @Test
    fun `play module routes are defined correctly`() {
        assertEquals("juega", BrailuxRoutes.PLAY)
        assertEquals("juega_memoria", BrailuxRoutes.PLAY_MEMORY)
        assertEquals("juega_secuencia", BrailuxRoutes.PLAY_SEQUENCE)
        assertEquals("juega_orden", BrailuxRoutes.PLAY_ORDER)
    }

    @Test
    fun `settings and about do not select home`() {
        assertNull(selectedMainDestination(BrailuxRoutes.SETTINGS))
        assertNull(selectedMainDestination(BrailuxRoutes.ABOUT))
        assertNull(selectedMainDestination(BrailuxRoutes.ASSISTANT))
    }

    @Test
    fun `secondary settings and about screens hide the bottom navigation`() {
        assertTrue(shouldShowBottomBar(BrailuxRoutes.HOME))
        assertTrue(shouldShowBottomBar(BrailuxRoutes.LEARN))
        assertTrue(shouldShowBottomBar(BrailuxRoutes.PLAY))
        assertTrue(shouldShowBottomBar(BrailuxRoutes.PROGRESS))
        assertFalse(shouldShowBottomBar(BrailuxRoutes.SETTINGS))
        assertFalse(shouldShowBottomBar(BrailuxRoutes.ABOUT))
        assertFalse(shouldShowBottomBar(BrailuxRoutes.DAILY_PRACTICE))
        assertFalse(shouldShowBottomBar(BrailuxRoutes.DAILY_CHALLENGE))
        assertFalse(shouldShowBottomBar(BrailuxRoutes.PLAY_MEMORY))
        assertFalse(shouldShowBottomBar(BrailuxRoutes.PLAY_SEQUENCE))
        assertFalse(shouldShowBottomBar(BrailuxRoutes.PLAY_ORDER))
        assertFalse(shouldShowBottomBar(null))
    }

    @Test
    fun `home navigation does not restore the removed secondary stack`() {
        assertFalse(shouldPreserveMainDestinationState(BrailuxRoutes.HOME))
        assertTrue(shouldPreserveMainDestinationState(BrailuxRoutes.LEARN))
        assertTrue(shouldPreserveMainDestinationState(BrailuxRoutes.PLAY))
        assertTrue(shouldPreserveMainDestinationState(BrailuxRoutes.PROGRESS))
    }

    @Test
    fun `lesson one continues to vowels`() {
        assertEquals(
            BrailuxRoutes.VOWELS_LESSON,
            nextLearningRoute(LearningLesson.SixDots),
        )
    }

    @Test
    fun `vowels continue to letters A to J`() {
        assertEquals(
            BrailuxRoutes.LETTERS_A_TO_J_LESSON,
            nextLearningRoute(LearningLesson.Vowels),
        )
    }

    @Test
    fun `letters A to J continue to letters K to T`() {
        assertEquals(
            BrailuxRoutes.LETTERS_K_TO_T_LESSON,
            nextLearningRoute(LearningLesson.LettersAtoJ),
        )
    }

    @Test
    fun `lesson four continues to lesson five`() {
        assertEquals(
            BrailuxRoutes.LETTERS_U_TO_Z_AND_ENYE_LESSON,
            nextLearningRoute(LearningLesson.LettersKtoT),
        )
    }

    @Test
    fun `alphabet route ends after lesson five and practice reuses its main destination`() {
        assertNull(nextLearningRoute(LearningLesson.LettersUtoZAndEnye))
        assertEquals(BrailuxRoutes.PRACTICE, alphabetPracticeRoute())
    }

    @Test
    fun `back from a guided lesson returns to the learning path`() {
        assertEquals(BrailuxRoutes.LEARN, learningParentRoute())
    }

    @Test
    fun `future lessons use their own real destinations`() {
        assertEquals(
            BrailuxRoutes.LETTERS_K_TO_T_LESSON,
            learningRouteFor(LearningLesson.LettersKtoT),
        )
        assertEquals(
            BrailuxRoutes.LETTERS_U_TO_Z_AND_ENYE_LESSON,
            learningRouteFor(LearningLesson.LettersUtoZAndEnye),
        )
    }
}
