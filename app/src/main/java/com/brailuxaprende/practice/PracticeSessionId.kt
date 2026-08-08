package com.brailuxaprende.practice

import java.util.UUID

fun newPracticeSessionId(): String = UUID.randomUUID().toString()

internal fun isValidPracticeSessionId(value: String): Boolean =
    value.isNotBlank() && value.length <= MAX_PRACTICE_SESSION_ID_LENGTH

private const val MAX_PRACTICE_SESSION_ID_LENGTH = 128
