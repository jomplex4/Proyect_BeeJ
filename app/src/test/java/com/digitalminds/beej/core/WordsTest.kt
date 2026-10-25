package com.digitalminds.beej.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordsTest {
    private val expected = listOf(
        "SINGER", "LAWYER", "ALWAYS", "WAITER", "REALLY", "PEOPLE", "DOCTOR", "PILOT", "SUGAR", "COFFEE",
        "MUSICIAN", "BEAUTIFUL", "EXCITED", "EXPENSIVE", "NORMALLY", "POLICEMAN", "RECORDING", "CAREFUL", "PINEAPPLE", "DANGEROUS",
        "WATERMELON", "INTERESTING", "UNCOUNTABLE", "PARTICIPATION", "PAPARAZZO", "DICTIONARY", "PRONUNCIATION", "COMPREHENSION", "COMMUNITY", "INTELLIGENT"
    )

    @Test fun hasExactlyThirtyWords() {
        assertEquals(30, Words.all.size)
    }

    @Test fun listMatchesOfficialSheetInOrder() {
        assertEquals(expected, Words.all.map { it.text })
    }

    @Test fun noDuplicatesAndNoExtraWords() {
        assertEquals(30, Words.all.map { it.text }.toSet().size)
        assertTrue(Words.all.all { it.text in expected })
    }

    @Test fun carefulIsSpelledCorrectly() {
        assertEquals("CAREFUL", Words.all[17].text)
        assertNull(Words.find("CARIFUL"))
        assertTrue(Words.find("careful") != null)
    }

    @Test fun onlyLettersAndEveryWordHasTranslation() {
        assertTrue(Words.all.all { w -> w.text.all { it in 'A'..'Z' } })
        assertTrue(Words.all.all { it.es.isNotBlank() })
    }

    @Test fun clipNamesAreLowercaseUnique() {
        assertEquals(30, Words.all.map { it.clip }.toSet().size)
        assertEquals("w_careful", Words.find("CAREFUL")!!.clip)
    }

    @Test fun grammarContainsAllWordsLettersAndDouble() {
        val g = SpokenLetters.grammarTokens()
        assertTrue(g.containsAll(Words.spokenForms))
        assertTrue("double" in g)
        for (c in 'A'..'Z') assertTrue("letra $c", SpokenLetters.variants[c]!!.all { it in g })
        assertFalse(g.any { it != it.lowercase() })
    }
}
