package com.brailuxaprende.braille

object BrailleRepository {
    private val vowels = listOf(
        BrailleCharacter(
            printedCharacter = 'A',
            cell = BrailleCell.fromPoints(setOf(1)),
            accessibleDescription = "Letra A: punto 1",
        ),
        BrailleCharacter(
            printedCharacter = 'E',
            cell = BrailleCell.fromPoints(setOf(1, 5)),
            accessibleDescription = "Letra E: puntos 1 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'I',
            cell = BrailleCell.fromPoints(setOf(2, 4)),
            accessibleDescription = "Letra I: puntos 2 y 4",
        ),
        BrailleCharacter(
            printedCharacter = 'O',
            cell = BrailleCell.fromPoints(setOf(1, 3, 5)),
            accessibleDescription = "Letra O: puntos 1, 3 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'U',
            cell = BrailleCell.fromPoints(setOf(1, 3, 6)),
            accessibleDescription = "Letra U: puntos 1, 3 y 6",
        ),
    )

    fun getVowels(): List<BrailleCharacter> = vowels

    fun findVowel(character: Char): BrailleCharacter? {
        val normalizedCharacter = character.uppercaseChar()
        return vowels.firstOrNull { it.printedCharacter == normalizedCharacter }
    }
}
