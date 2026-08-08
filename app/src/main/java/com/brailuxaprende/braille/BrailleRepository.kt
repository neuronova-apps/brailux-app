package com.brailuxaprende.braille

object BrailleRepository {
    private val characters = listOf(
        BrailleCharacter(
            printedCharacter = 'A',
            cell = BrailleCell.fromPoints(setOf(1)),
            accessibleDescription = "Letra A: punto 1",
        ),
        BrailleCharacter(
            printedCharacter = 'B',
            cell = BrailleCell.fromPoints(setOf(1, 2)),
            accessibleDescription = "Letra B: puntos 1 y 2",
        ),
        BrailleCharacter(
            printedCharacter = 'C',
            cell = BrailleCell.fromPoints(setOf(1, 4)),
            accessibleDescription = "Letra C: puntos 1 y 4",
        ),
        BrailleCharacter(
            printedCharacter = 'D',
            cell = BrailleCell.fromPoints(setOf(1, 4, 5)),
            accessibleDescription = "Letra D: puntos 1, 4 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'E',
            cell = BrailleCell.fromPoints(setOf(1, 5)),
            accessibleDescription = "Letra E: puntos 1 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'F',
            cell = BrailleCell.fromPoints(setOf(1, 2, 4)),
            accessibleDescription = "Letra F: puntos 1, 2 y 4",
        ),
        BrailleCharacter(
            printedCharacter = 'G',
            cell = BrailleCell.fromPoints(setOf(1, 2, 4, 5)),
            accessibleDescription = "Letra G: puntos 1, 2, 4 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'H',
            cell = BrailleCell.fromPoints(setOf(1, 2, 5)),
            accessibleDescription = "Letra H: puntos 1, 2 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'I',
            cell = BrailleCell.fromPoints(setOf(2, 4)),
            accessibleDescription = "Letra I: puntos 2 y 4",
        ),
        BrailleCharacter(
            printedCharacter = 'J',
            cell = BrailleCell.fromPoints(setOf(2, 4, 5)),
            accessibleDescription = "Letra J: puntos 2, 4 y 5",
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

    private val vowels = characters.filter { it.printedCharacter in setOf('A', 'E', 'I', 'O', 'U') }
    private val level1Characters = characters.filter { it.printedCharacter in 'A'..'J' }

    fun getVowels(): List<BrailleCharacter> = vowels

    fun getLevel1Characters(): List<BrailleCharacter> = level1Characters

    fun findCharacter(character: Char): BrailleCharacter? {
        val normalizedCharacter = character.uppercaseChar()
        return characters.firstOrNull { it.printedCharacter == normalizedCharacter }
    }

    fun findVowel(character: Char): BrailleCharacter? {
        val normalizedCharacter = character.uppercaseChar()
        return vowels.firstOrNull { it.printedCharacter == normalizedCharacter }
    }
}
