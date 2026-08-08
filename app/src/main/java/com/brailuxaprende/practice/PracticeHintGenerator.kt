package com.brailuxaprende.practice

import com.brailuxaprende.braille.BrailleCell

object PracticeHintGenerator {
    private val leftColumn = setOf(1, 2, 3)
    private val rightColumn = setOf(4, 5, 6)
    private val rowPoints = linkedMapOf(
        BrailleRow.Top to setOf(1, 4),
        BrailleRow.Middle to setOf(2, 5),
        BrailleRow.Bottom to setOf(3, 6),
    )

    fun generate(level: PracticeLevel, cell: BrailleCell): List<PracticeHint> {
        val activePoints = cell.activePoints().toSet()
        return when (level) {
            PracticeLevel.BrailleExplorer -> level1Hints(activePoints)
            PracticeLevel.BrailleRecognizer -> level2Hints(activePoints)
            PracticeLevel.BrailleChallenge -> emptyList()
        }
    }

    private fun level1Hints(activePoints: Set<Int>): List<PracticeHint> {
        val structuralHint = if (activePoints.size >= 4) {
            usedRowState(activePoints)
        } else {
            columnDistribution(activePoints)
        }
        val detailedPointCandidates = when (structuralHint) {
            is PracticeHint.RowState -> {
                val selectedRowPoints = rowPoints.getValue(structuralHint.row)
                if (structuralHint.activeCount == 1) {
                    selectedRowPoints
                } else {
                    (1..6).filterNot { it in selectedRowPoints }
                }
            }
            else -> 1..6
        }

        return listOf(
            PracticeHint.ActivePointCount(activePoints.size),
            structuralHint,
            pointState(activePoints, detailedPointCandidates, preferActive = true),
        ).distinct()
    }

    private fun level2Hints(activePoints: Set<Int>): List<PracticeHint> {
        val columnDistribution = columnDistribution(activePoints)
        val rowState = level2RowState(activePoints)
        val usedColumnPoints: Iterable<Int> = when {
            columnDistribution.leftCount > 0 && columnDistribution.rightCount == 0 -> leftColumn
            columnDistribution.rightCount > 0 && columnDistribution.leftCount == 0 -> rightColumn
            else -> 1..6
        }
        val detailedPointCandidates = usedColumnPoints.filterNot { point ->
            point in rowPoints.getValue(rowState.row)
        }

        return listOf(
            columnDistribution,
            rowState,
            pointState(activePoints, detailedPointCandidates, preferActive = false),
        ).distinct()
    }

    private fun columnDistribution(activePoints: Set<Int>): PracticeHint.ColumnDistribution =
        PracticeHint.ColumnDistribution(
            leftCount = activePoints.count { it in leftColumn },
            rightCount = activePoints.count { it in rightColumn },
        )

    private fun usedRowState(activePoints: Set<Int>): PracticeHint.RowState {
        val selectedRow = rowPoints.entries
            .map { entry -> entry to activePoints.count { it in entry.value } }
            .filter { (_, activeCount) -> activeCount > 0 }
            .minByOrNull { (_, activeCount) -> activeCount }
            ?: (rowPoints.entries.first() to 0)
        return PracticeHint.RowState(
            row = selectedRow.first.key,
            activeCount = selectedRow.second,
        )
    }

    private fun level2RowState(activePoints: Set<Int>): PracticeHint.RowState {
        val selectedRow = rowPoints.entries
            .firstOrNull { (_, points) -> activePoints.intersect(points).isEmpty() }
            ?: rowPoints.entries.first()
        return PracticeHint.RowState(
            row = selectedRow.key,
            activeCount = activePoints.count { it in selectedRow.value },
        )
    }

    private fun pointState(
        activePoints: Set<Int>,
        candidates: Iterable<Int>,
        preferActive: Boolean,
    ): PracticeHint.PointState {
        val candidatePoints = candidates.toList()
        val preferredPoint = if (preferActive) {
            candidatePoints.firstOrNull { it in activePoints }
        } else {
            candidatePoints.lastOrNull { it !in activePoints }
        }
        val point = preferredPoint
            ?: candidatePoints.firstOrNull { it in activePoints }
            ?: candidatePoints.first()
        return PracticeHint.PointState(
            point = point,
            isActive = point in activePoints,
        )
    }
}
