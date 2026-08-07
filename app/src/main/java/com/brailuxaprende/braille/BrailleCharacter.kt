package com.brailuxaprende.braille

data class BrailleCharacter(
    val printedCharacter: Char,
    val cell: BrailleCell,
    val accessibleDescription: String,
)
