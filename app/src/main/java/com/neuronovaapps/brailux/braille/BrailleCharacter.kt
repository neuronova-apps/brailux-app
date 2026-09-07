package com.neuronovaapps.brailux.braille

data class BrailleCharacter(
    val printedCharacter: Char,
    val cell: BrailleCell,
    val accessibleDescription: String,
)
