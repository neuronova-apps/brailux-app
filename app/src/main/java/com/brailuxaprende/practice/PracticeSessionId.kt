package com.brailuxaprende.practice

import java.util.UUID

fun newPracticeSessionId(): String = UUID.randomUUID().toString()

fun dailyPracticeSessionId(date: PracticeDate): String = "daily_${date.isoValue}"

fun isDailyPracticeSessionId(id: String, date: PracticeDate): Boolean =
    id == dailyPracticeSessionId(date)

fun parseDailyPracticeDate(id: String): PracticeDate? {
    if (!id.startsWith("daily_")) return null
    return PracticeDate.parse(id.removePrefix("daily_"))
}

internal fun isValidPracticeSessionId(value: String): Boolean =
    value.isNotBlank() && value.length <= MAX_PRACTICE_SESSION_ID_LENGTH

private const val MAX_PRACTICE_SESSION_ID_LENGTH = 128
