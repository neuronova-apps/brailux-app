package com.brailuxaprende.data.practice

internal data class StoredPracticeDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

internal fun parseStoredPracticeDate(value: String?): StoredPracticeDate? {
    val match = StoredDatePattern.matchEntire(value ?: return null) ?: return null
    val year = match.groupValues[1].toInt()
    val month = match.groupValues[2].toInt()
    val day = match.groupValues[3].toInt()
    if (month !in 1..12 || day !in 1..daysInMonth(year, month)) return null

    return StoredPracticeDate(year = year, month = month, day = day)
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (isLeapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun isLeapYear(year: Int): Boolean =
    year % 400 == 0 || year % 4 == 0 && year % 100 != 0

private val StoredDatePattern = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
