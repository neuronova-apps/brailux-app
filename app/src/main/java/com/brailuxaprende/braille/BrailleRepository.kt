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
            printedCharacter = 'K',
            cell = BrailleCell.fromPoints(setOf(1, 3)),
            accessibleDescription = "Letra K: puntos 1 y 3",
        ),
        BrailleCharacter(
            printedCharacter = 'L',
            cell = BrailleCell.fromPoints(setOf(1, 2, 3)),
            accessibleDescription = "Letra L: puntos 1, 2 y 3",
        ),
        BrailleCharacter(
            printedCharacter = 'M',
            cell = BrailleCell.fromPoints(setOf(1, 3, 4)),
            accessibleDescription = "Letra M: puntos 1, 3 y 4",
        ),
        BrailleCharacter(
            printedCharacter = 'N',
            cell = BrailleCell.fromPoints(setOf(1, 3, 4, 5)),
            accessibleDescription = "Letra N: puntos 1, 3, 4 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'Ñ',
            cell = BrailleCell.fromPoints(setOf(1, 2, 4, 5, 6)),
            accessibleDescription = "Letra Ñ: puntos 1, 2, 4, 5 y 6",
        ),
        BrailleCharacter(
            printedCharacter = 'O',
            cell = BrailleCell.fromPoints(setOf(1, 3, 5)),
            accessibleDescription = "Letra O: puntos 1, 3 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'P',
            cell = BrailleCell.fromPoints(setOf(1, 2, 3, 4)),
            accessibleDescription = "Letra P: puntos 1, 2, 3 y 4",
        ),
        BrailleCharacter(
            printedCharacter = 'Q',
            cell = BrailleCell.fromPoints(setOf(1, 2, 3, 4, 5)),
            accessibleDescription = "Letra Q: puntos 1, 2, 3, 4 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'R',
            cell = BrailleCell.fromPoints(setOf(1, 2, 3, 5)),
            accessibleDescription = "Letra R: puntos 1, 2, 3 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'S',
            cell = BrailleCell.fromPoints(setOf(2, 3, 4)),
            accessibleDescription = "Letra S: puntos 2, 3 y 4",
        ),
        BrailleCharacter(
            printedCharacter = 'T',
            cell = BrailleCell.fromPoints(setOf(2, 3, 4, 5)),
            accessibleDescription = "Letra T: puntos 2, 3, 4 y 5",
        ),
        BrailleCharacter(
            printedCharacter = 'U',
            cell = BrailleCell.fromPoints(setOf(1, 3, 6)),
            accessibleDescription = "Letra U: puntos 1, 3 y 6",
        ),
        BrailleCharacter(
            printedCharacter = 'V',
            cell = BrailleCell.fromPoints(setOf(1, 2, 3, 6)),
            accessibleDescription = "Letra V: puntos 1, 2, 3 y 6",
        ),
        BrailleCharacter(
            printedCharacter = 'W',
            cell = BrailleCell.fromPoints(setOf(2, 4, 5, 6)),
            accessibleDescription = "Letra W: puntos 2, 4, 5 y 6",
        ),
        BrailleCharacter(
            printedCharacter = 'X',
            cell = BrailleCell.fromPoints(setOf(1, 3, 4, 6)),
            accessibleDescription = "Letra X: puntos 1, 3, 4 y 6",
        ),
        BrailleCharacter(
            printedCharacter = 'Y',
            cell = BrailleCell.fromPoints(setOf(1, 3, 4, 5, 6)),
            accessibleDescription = "Letra Y: puntos 1, 3, 4, 5 y 6",
        ),
        BrailleCharacter(
            printedCharacter = 'Z',
            cell = BrailleCell.fromPoints(setOf(1, 3, 5, 6)),
            accessibleDescription = "Letra Z: puntos 1, 3, 5 y 6",
        ),
    )

    private val vowels = characters.filter { it.printedCharacter in setOf('A', 'E', 'I', 'O', 'U') }
    private val level1Characters = characters.filter { it.printedCharacter in 'A'..'J' }
    private val lettersKToT = characters.filter { it.printedCharacter in 'K'..'T' }
    private val lettersUToZAndEnye = (('U'..'Z').toList() + 'Ñ').map { character ->
        requireNotNull(characters.firstOrNull { it.printedCharacter == character })
    }
    private val spanishAlphabet = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".toList()
    private val level2Characters = spanishAlphabet.map { character ->
        requireNotNull(characters.firstOrNull { it.printedCharacter == character })
    }

    fun getVowels(): List<BrailleCharacter> = vowels

    fun getLevel1Characters(): List<BrailleCharacter> = level1Characters

    fun getLettersKToT(): List<BrailleCharacter> = lettersKToT

    fun getLettersUToZAndEnye(): List<BrailleCharacter> = lettersUToZAndEnye

    fun getLevel2Characters(): List<BrailleCharacter> = level2Characters

    fun findCharacter(character: Char): BrailleCharacter? {
        val normalizedCharacter = character.uppercaseChar()
        return characters.firstOrNull { it.printedCharacter == normalizedCharacter }
    }

    fun findVowel(character: Char): BrailleCharacter? {
        val normalizedCharacter = character.uppercaseChar()
        return vowels.firstOrNull { it.printedCharacter == normalizedCharacter }
    }
}
