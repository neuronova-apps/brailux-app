package com.brailuxaprende.braille

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class BrailleRepositoryTest {
    @Test
    fun returnsTheFiveInitialVowels() {
        val vowels = BrailleRepository.getVowels()

        assertEquals(listOf('A', 'E', 'I', 'O', 'U'), vowels.map { it.printedCharacter })
        assertEquals(5, vowels.size)
    }

    @Test
    fun mapsInitialVowelsToCorrectBrailleCells() {
        assertEquals(listOf(1), BrailleRepository.findVowel('A')?.cell?.activePoints())
        assertEquals(listOf(1, 5), BrailleRepository.findVowel('E')?.cell?.activePoints())
        assertEquals(listOf(2, 4), BrailleRepository.findVowel('I')?.cell?.activePoints())
        assertEquals(listOf(1, 3, 5), BrailleRepository.findVowel('O')?.cell?.activePoints())
        assertEquals(listOf(1, 3, 6), BrailleRepository.findVowel('U')?.cell?.activePoints())
    }

    @Test
    fun includesAccessibleDescriptionsInSpanish() {
        assertEquals("Letra A: punto 1", BrailleRepository.findVowel('A')?.accessibleDescription)
        assertEquals("Letra E: puntos 1 y 5", BrailleRepository.findVowel('E')?.accessibleDescription)
        assertEquals("Letra I: puntos 2 y 4", BrailleRepository.findVowel('I')?.accessibleDescription)
        assertEquals("Letra O: puntos 1, 3 y 5", BrailleRepository.findVowel('O')?.accessibleDescription)
        assertEquals("Letra U: puntos 1, 3 y 6", BrailleRepository.findVowel('U')?.accessibleDescription)
    }

    @Test
    fun findsVowelsIgnoringCase() {
        assertSame(BrailleRepository.findVowel('A'), BrailleRepository.findVowel('a'))
        assertSame(BrailleRepository.findVowel('E'), BrailleRepository.findVowel('e'))
        assertSame(BrailleRepository.findVowel('I'), BrailleRepository.findVowel('i'))
        assertSame(BrailleRepository.findVowel('O'), BrailleRepository.findVowel('o'))
        assertSame(BrailleRepository.findVowel('U'), BrailleRepository.findVowel('u'))
    }

    @Test
    fun returnsSafeResultForUnregisteredCharacter() {
        assertNull(BrailleRepository.findVowel('B'))
    }
}
