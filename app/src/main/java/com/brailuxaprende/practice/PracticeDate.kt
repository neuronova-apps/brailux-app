package com.brailuxaprende.practice

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

data class PracticeDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<PracticeDate> {
    init {
        require(year in 1..9999) { "Year must be between 1 and 9999." }
        require(month in 1..12) { "Month must be between 1 and 12." }
        require(day in 1..daysInMonth(year, month)) { "Day is not valid for the month." }
    }

    val isoValue: String
        get() = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)

    val monthKey: String
        get() = String.format(Locale.ROOT, "%04d-%02d", year, month)

    val epochDay: Long
        get() = Math.floorDiv(calendar().timeInMillis, MillisecondsPerDay)

    val weekStart: PracticeDate
        get() {
            val calendar = calendar()
            val offsetFromMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
            calendar.add(Calendar.DAY_OF_MONTH, -offsetFromMonday)
            return calendar.toPracticeDate()
        }

    fun plusDays(days: Int): PracticeDate {
        val calendar = calendar()
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.toPracticeDate()
    }

    override fun compareTo(other: PracticeDate): Int = compareValuesBy(
        this,
        other,
        PracticeDate::year,
        PracticeDate::month,
        PracticeDate::day,
    )

    private fun calendar(): Calendar = GregorianCalendar(Utc).apply {
        isLenient = false
        clear()
        set(year, month - 1, day, 12, 0, 0)
    }

    companion object {
        fun from(calendar: Calendar): PracticeDate = PracticeDate(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
        )

        fun parse(value: String?): PracticeDate? {
            val match = IsoPattern.matchEntire(value ?: return null) ?: return null
            return runCatching {
                PracticeDate(
                    year = match.groupValues[1].toInt(),
                    month = match.groupValues[2].toInt(),
                    day = match.groupValues[3].toInt(),
                )
            }.getOrNull()
        }

        private const val MillisecondsPerDay = 86_400_000L
        private val IsoPattern = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
        private val Utc: TimeZone = TimeZone.getTimeZone("UTC")
    }
}

fun interface PracticeClock {
    fun today(): PracticeDate
}

object SystemPracticeClock : PracticeClock {
    override fun today(): PracticeDate = PracticeDate.from(Calendar.getInstance())
}

private fun Calendar.toPracticeDate(): PracticeDate = PracticeDate(
    year = get(Calendar.YEAR),
    month = get(Calendar.MONTH) + 1,
    day = get(Calendar.DAY_OF_MONTH),
)

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (isLeapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun isLeapYear(year: Int): Boolean =
    year % 400 == 0 || year % 4 == 0 && year % 100 != 0
