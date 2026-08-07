package com.brailuxaprende.braille

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailleCellTest {
    @Test
    fun acceptsValidPointNumbersFromOneToSix() {
        val cell = BrailleCell.fromPoints(setOf(1, 2, 3, 4, 5, 6))

        assertEquals(listOf(1, 2, 3, 4, 5, 6), cell.activePoints())
        assertTrue(cell.isPointActive(1))
        assertTrue(cell.isPointActive(6))
    }

    @Test
    fun rejectsPointZero() {
        assertThrows(IllegalArgumentException::class.java) {
            BrailleCell.fromPoints(setOf(0))
        }
    }

    @Test
    fun rejectsPointSeven() {
        assertThrows(IllegalArgumentException::class.java) {
            BrailleCell.fromPoints(setOf(7))
        }
    }

    @Test
    fun rejectsPointQueriesOutsideValidRange() {
        val cell = BrailleCell.fromPoints(setOf(1))

        assertThrows(IllegalArgumentException::class.java) {
            cell.isPointActive(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            cell.isPointActive(7)
        }
    }

    @Test
    fun returnsActivePointsOrdered() {
        val cell = BrailleCell.fromPoints(setOf(6, 1, 3))

        assertEquals(listOf(1, 3, 6), cell.activePoints())
    }

    @Test
    fun comparesEquivalentCellsByContent() {
        val firstCell = BrailleCell.fromPoints(setOf(1, 3, 6))
        val secondCell = BrailleCell.fromPoints(setOf(6, 3, 1))

        assertEquals(firstCell, secondCell)
    }

    @Test
    fun storesDefensiveCopyOfPoints() {
        val mutablePoints = mutableSetOf(1)
        val cell = BrailleCell.fromPoints(mutablePoints)

        mutablePoints.add(6)

        assertEquals(listOf(1), cell.activePoints())
        assertFalse(cell.isPointActive(6))
    }
}
