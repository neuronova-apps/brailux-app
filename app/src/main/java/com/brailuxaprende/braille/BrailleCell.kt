package com.brailuxaprende.braille

class BrailleCell private constructor(
    private val points: Set<Int>,
) {
    fun isPointActive(point: Int): Boolean {
        require(point in VALID_POINT_RANGE) { "Braille point must be between 1 and 6." }
        return point in points
    }

    fun activePoints(): List<Int> = points.sorted()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrailleCell) return false
        return points == other.points
    }

    override fun hashCode(): Int = points.hashCode()

    override fun toString(): String = "BrailleCell(points=${activePoints()})"

    companion object {
        private val VALID_POINT_RANGE = 1..6

        fun fromPoints(points: Set<Int>): BrailleCell {
            require(points.all { it in VALID_POINT_RANGE }) {
                "Braille points must be between 1 and 6."
            }
            return BrailleCell(points.toSet())
        }
    }
}
