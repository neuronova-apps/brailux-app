package com.brailuxaprende.practice

object PracticeHintGenerator {
    private const val spanishAlphabet = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ"
    private val vowels = setOf('A', 'E', 'I', 'O', 'U')
    private val leftColumn = setOf(1, 2, 3)
    private val rightColumn = setOf(4, 5, 6)
    private val rowPoints = linkedMapOf(
        BrailleRow.Top to setOf(1, 4),
        BrailleRow.Middle to setOf(2, 5),
        BrailleRow.Bottom to setOf(3, 6),
    )

    fun generate(level: PracticeLevel, exercise: PracticeExercise): List<PracticeHint> {
        if (level == PracticeLevel.BrailleChallenge) return emptyList()

        return when (exercise.type) {
            PracticeExerciseType.CharacterToSign -> structuralHints(
                level = level,
                activePoints = exercise.target.cell.activePoints().toSet(),
            )
            PracticeExerciseType.SignToCharacter -> characterHints(level, exercise)
        }
    }

    private fun structuralHints(
        level: PracticeLevel,
        activePoints: Set<Int>,
    ): List<PracticeHint> {
        return when (level) {
            PracticeLevel.BrailleExplorer -> level1Hints(activePoints)
            PracticeLevel.BrailleRecognizer -> level2Hints(activePoints)
            PracticeLevel.BrailleChallenge -> emptyList()
        }
    }

    private fun characterHints(
        level: PracticeLevel,
        exercise: PracticeExercise,
    ): List<PracticeHint> {
        val target = exercise.target.printedCharacter
        val category = PracticeHint.CharacterCategory(isVowel = target in vowels)
        val range = alphabetRange(level, target)
        val comparison = alphabetComparison(target, exercise.options.map { it.printedCharacter })
        val prioritizedHints = when (level) {
            PracticeLevel.BrailleExplorer -> listOf(category, range, comparison)
            PracticeLevel.BrailleRecognizer -> listOf(range, category, comparison)
            PracticeLevel.BrailleChallenge -> emptyList()
        }
        return prioritizedHints
            .distinct()
            .sortedByDescending { hint -> reducesVisibleOptions(hint, exercise) }
    }

    private fun reducesVisibleOptions(
        hint: PracticeHint,
        exercise: PracticeExercise,
    ): Boolean {
        val matchingOptions = exercise.options.count { option ->
            val character = option.printedCharacter
            when (hint) {
                is PracticeHint.CharacterCategory -> (character in vowels) == hint.isVowel
                is PracticeHint.AlphabetRange -> {
                    spanishAlphabet.indexOf(character) in
                        spanishAlphabet.indexOf(hint.first)..spanishAlphabet.indexOf(hint.last)
                }
                is PracticeHint.AlphabetComparison -> if (hint.targetComesAfter) {
                    spanishAlphabet.indexOf(character) > spanishAlphabet.indexOf(hint.reference)
                } else {
                    spanishAlphabet.indexOf(character) < spanishAlphabet.indexOf(hint.reference)
                }
                else -> true
            }
        }
        return matchingOptions in 1 until exercise.options.size
    }

    private fun alphabetRange(level: PracticeLevel, target: Char): PracticeHint.AlphabetRange {
        val targetIndex = spanishAlphabet.indexOf(target)
        val ranges = when (level) {
            PracticeLevel.BrailleExplorer -> listOf(0..4, 5..9)
            PracticeLevel.BrailleRecognizer,
            PracticeLevel.BrailleChallenge -> listOf(0..9, 10..18, 19..26)
        }
        val range = ranges.first { targetIndex in it }
        return PracticeHint.AlphabetRange(
            first = spanishAlphabet[range.first],
            last = spanishAlphabet[range.last],
        )
    }

    private fun alphabetComparison(
        target: Char,
        visibleOptions: List<Char>,
    ): PracticeHint.AlphabetComparison {
        val orderedOptions = visibleOptions.sortedBy(spanishAlphabet::indexOf)
        val targetIndex = orderedOptions.indexOf(target)
        return if (targetIndex >= 2) {
            PracticeHint.AlphabetComparison(
                reference = orderedOptions[targetIndex - 2],
                targetComesAfter = true,
            )
        } else {
            PracticeHint.AlphabetComparison(
                reference = orderedOptions[targetIndex + 2],
                targetComesAfter = false,
            )
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
