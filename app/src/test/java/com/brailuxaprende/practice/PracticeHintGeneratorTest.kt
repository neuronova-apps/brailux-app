package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCell
import com.brailuxaprende.braille.BrailleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeHintGeneratorTest {
    @Test
    fun level1UsesCountThenColumnsThenPoint() {
        val hints = PracticeHintGenerator.generate(
            PracticeLevel.BrailleExplorer,
            BrailleCell.fromPoints(setOf(1, 2, 5)),
        )

        assertTrue(hints[0] is PracticeHint.ActivePointCount)
        assertTrue(hints[1] is PracticeHint.ColumnDistribution)
        assertTrue(hints[2] is PracticeHint.PointState)
        assertEquals(PracticeHint.ActivePointCount(3), hints[0])
        assertEquals(PracticeHint.ColumnDistribution(leftCount = 2, rightCount = 1), hints[1])
        assertEquals(PracticeHint.PointState(point = 1, isActive = true), hints[2])
    }

    @Test
    fun level1UsesARowHintWhenPointCountAlreadyImpliesBothColumns() {
        val hints = PracticeHintGenerator.generate(
            PracticeLevel.BrailleExplorer,
            BrailleCell.fromPoints(setOf(1, 2, 4, 5)),
        )

        assertEquals(PracticeHint.ActivePointCount(4), hints[0])
        assertEquals(PracticeHint.RowState(BrailleRow.Top, activeCount = 2), hints[1])
        assertEquals(PracticeHint.PointState(point = 2, isActive = true), hints[2])
    }

    @Test
    fun level2UsesColumnsThenRowThenDetailOutsideThatRow() {
        val hints = PracticeHintGenerator.generate(
            PracticeLevel.BrailleRecognizer,
            BrailleCell.fromPoints(setOf(1, 2, 3)),
        )

        assertTrue(hints[0] is PracticeHint.ColumnDistribution)
        assertEquals(PracticeHint.RowState(BrailleRow.Top, activeCount = 1), hints[1])
        assertEquals(PracticeHint.PointState(point = 2, isActive = true), hints[2])
        assertFalse((hints[2] as PracticeHint.PointState).point in setOf(1, 4))
    }

    @Test
    fun level2SpecificHintCanDescribeAnAbsentPointTruthfully() {
        val hints = PracticeHintGenerator.generate(
            PracticeLevel.BrailleRecognizer,
            BrailleCell.fromPoints(setOf(1, 2, 4)),
        )

        assertEquals(PracticeHint.PointState(point = 5, isActive = false), hints[2])
    }

    @Test
    fun level2DetailDoesNotRepeatAnUnusedColumn() {
        val hints = PracticeHintGenerator.generate(
            PracticeLevel.BrailleRecognizer,
            BrailleCell.fromPoints(setOf(1, 2, 3)),
        )

        val detail = hints[2] as PracticeHint.PointState
        assertTrue(detail.point in 1..3)
    }

    @Test
    fun level2PrefersATrueAbsentRowWhenAvailable() {
        val hints = PracticeHintGenerator.generate(
            PracticeLevel.BrailleRecognizer,
            BrailleCell.fromPoints(setOf(1, 2, 4)),
        )

        assertEquals(PracticeHint.RowState(BrailleRow.Bottom, activeCount = 0), hints[1])
    }

    @Test
    fun generatedHintsAreDeterministicTrueAndNotRepeatedForEveryCell() {
        for (mask in 0 until (1 shl 6)) {
            val points = (1..6).filterTo(mutableSetOf()) { point ->
                mask and (1 shl (point - 1)) != 0
            }
            val cell = BrailleCell.fromPoints(points)

            listOf(PracticeLevel.BrailleExplorer, PracticeLevel.BrailleRecognizer).forEach { level ->
                val firstGeneration = PracticeHintGenerator.generate(level, cell)
                val secondGeneration = PracticeHintGenerator.generate(level, cell)

                assertEquals(firstGeneration, secondGeneration)
                assertEquals(3, firstGeneration.size)
                assertEquals(firstGeneration.size, firstGeneration.distinct().size)
                firstGeneration.forEach { hint -> assertHintIsTrue(hint, points) }
            }
        }
    }

    @Test
    fun everyHintReducesUncertaintyForEveryPracticeTarget() {
        val allPointPatterns = (0 until (1 shl 6)).map { mask ->
            (1..6).filterTo(mutableSetOf()) { point ->
                mask and (1 shl (point - 1)) != 0
            }
        }
        val targetsByLevel = mapOf(
            PracticeLevel.BrailleExplorer to BrailleRepository.getLevel1Characters(),
            PracticeLevel.BrailleRecognizer to BrailleRepository.getLevel2Characters(),
        )

        targetsByLevel.forEach { (level, targets) ->
            targets.forEach { target ->
                var candidates = allPointPatterns
                PracticeHintGenerator.generate(level, target.cell).forEach { hint ->
                    val reducedCandidates = candidates.filter { points ->
                        matchesDisplayedHint(hint, points)
                    }
                    assertTrue(
                        "Hint $hint must reduce uncertainty for ${target.printedCharacter}",
                        reducedCandidates.size < candidates.size,
                    )
                    candidates = reducedCandidates
                }
            }
        }
    }

    @Test
    fun level3AlwaysReturnsAnEmptyList() {
        for (mask in 0 until (1 shl 6)) {
            val points = (1..6).filterTo(mutableSetOf()) { point ->
                mask and (1 shl (point - 1)) != 0
            }

            assertTrue(
                PracticeHintGenerator.generate(
                    PracticeLevel.BrailleChallenge,
                    BrailleCell.fromPoints(points),
                ).isEmpty(),
            )
        }
    }

    private fun assertHintIsTrue(hint: PracticeHint, activePoints: Set<Int>) {
        when (hint) {
            is PracticeHint.ActivePointCount -> assertEquals(activePoints.size, hint.count)
            is PracticeHint.ColumnDistribution -> {
                assertEquals(activePoints.count { it in 1..3 }, hint.leftCount)
                assertEquals(activePoints.count { it in 4..6 }, hint.rightCount)
            }
            is PracticeHint.RowState -> assertEquals(
                activePoints.count { it in pointsFor(hint.row) },
                hint.activeCount,
            )
            is PracticeHint.PointState -> assertEquals(hint.point in activePoints, hint.isActive)
        }
    }

    private fun matchesDisplayedHint(hint: PracticeHint, activePoints: Set<Int>): Boolean =
        when (hint) {
            is PracticeHint.ActivePointCount -> activePoints.size == hint.count
            is PracticeHint.ColumnDistribution -> {
                val usesLeftColumn = activePoints.any { it in 1..3 }
                val usesRightColumn = activePoints.any { it in 4..6 }
                when {
                    hint.leftCount > 0 && hint.rightCount > 0 ->
                        usesLeftColumn && usesRightColumn
                    hint.leftCount > 0 -> usesLeftColumn && !usesRightColumn
                    hint.rightCount > 0 -> !usesLeftColumn && usesRightColumn
                    else -> activePoints.isEmpty()
                }
            }
            is PracticeHint.RowState ->
                activePoints.count { it in pointsFor(hint.row) } == hint.activeCount
            is PracticeHint.PointState -> (hint.point in activePoints) == hint.isActive
        }

    private fun pointsFor(row: BrailleRow): Set<Int> = when (row) {
        BrailleRow.Top -> setOf(1, 4)
        BrailleRow.Middle -> setOf(2, 5)
        BrailleRow.Bottom -> setOf(3, 6)
    }
}
