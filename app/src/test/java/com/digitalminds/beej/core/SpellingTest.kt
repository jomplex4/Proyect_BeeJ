package com.digitalminds.beej.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellingTest {
    private fun heard(text: String, word: String) =
        SpellingEvaluator.evaluateTokens(word, SpokenLetters.tokenize(text))

    @Test fun everyLetterHasAtLeastTheLetterItselfAndNoVariantIsAmbiguous() {
        for (c in 'A'..'Z') assertEquals(c, SpokenLetters.tokenToLetter[c.lowercase()])
        val all = SpokenLetters.variants.values.flatten()
        assertEquals(all.size, all.toSet().size)
    }

    @Test fun normalizesSpokenVariants() {
        assertEquals('B', SpokenLetters.tokenToLetter["bee"])
        assertEquals('B', SpokenLetters.tokenToLetter["be"])
        assertEquals('C', SpokenLetters.tokenToLetter["see"])
        assertEquals('C', SpokenLetters.tokenToLetter["sea"])
        assertEquals('R', SpokenLetters.tokenToLetter["are"])
        assertEquals('U', SpokenLetters.tokenToLetter["you"])
        assertEquals('Y', SpokenLetters.tokenToLetter["why"])
        assertEquals('A', SpokenLetters.tokenToLetter["a"])
        assertEquals('I', SpokenLetters.tokenToLetter["eye"])
    }

    @Test fun exactSpellingIsPerfect() {
        val r = heard("c a r e f u l", "CAREFUL")
        assertEquals(Verdict.PERFECT, r.verdict)
        assertEquals(0, r.distance)
        assertEquals(-1, r.firstErrorIndex)
    }

    @Test fun spokenNamesAreRecognizedInsteadOfRawText() {
        val r = heard("see a are e ef you el", "CAREFUL")
        assertEquals(Verdict.PERFECT, r.verdict)
    }

    @Test fun oneSubstitutionIsAlmostAndLocated() {
        val r = heard("c a r i f u l", "CAREFUL")
        assertEquals(Verdict.ALMOST, r.verdict)
        assertEquals(1, r.distance)
        assertEquals(3, r.firstErrorIndex)
        assertEquals(Op.SUB, r.steps[3].op)
        assertEquals('E', r.steps[3].expected)
        assertEquals('I', r.steps[3].heard)
    }

    @Test fun missingLetterIsDetected() {
        val r = heard("c a r f u l", "CAREFUL")
        assertEquals(1, r.distance)
        assertEquals(Verdict.ALMOST, r.verdict)
        assertTrue(r.steps.any { it.op == Op.MISSING && it.expected == 'E' })
    }

    @Test fun extraLetterIsDetected() {
        val r = heard("c a r e e f u l", "CAREFUL")
        assertEquals(1, r.distance)
        assertTrue(r.steps.any { it.op == Op.EXTRA })
    }

    @Test fun twoOrMoreErrorsMeansReinforce() {
        val r = heard("c a r i f o l", "CAREFUL")
        assertEquals(Verdict.REINFORCE, r.verdict)
        assertEquals(2, r.distance)
    }

    @Test fun emptyIsReinforce() {
        val r = heard("", "PILOT")
        assertEquals(Verdict.REINFORCE, r.verdict)
        assertEquals(5, r.distance)
    }

    @Test fun unknownTokensCountAsErrors() {
        val r = heard("p i banana o t", "PILOT")
        assertTrue(r.distance >= 1)
        assertTrue(r.verdict != Verdict.PERFECT)
    }

    @Test fun doubleLetterFormat() {
        assertEquals(Verdict.PERFECT, heard("c o double f e e", "COFFEE").verdict)
        assertEquals(Verdict.PERFECT, heard("c o f f e e", "COFFEE").verdict)
        assertEquals(Verdict.PERFECT, heard("c o f f double e", "COFFEE").verdict)
        assertEquals(Verdict.PERFECT, heard("c o double f double e", "COFFEE").verdict)
    }

    @Test fun doubleOnAWrongLetterIsAnError() {
        assertTrue(heard("c o double g e e", "COFFEE").distance > 0)
        assertTrue(heard("c o double e e", "COFFEE").distance > 0)
    }

    @Test fun doubleWithoutLetterIsAnError() {
        assertTrue(heard("c o f f e double", "COFFEE").distance > 0)
    }

    @Test fun doubleUMeansLetterW() {
        assertEquals(Verdict.PERFECT, heard("l a double u y e r", "LAWYER").verdict)
        assertEquals(Verdict.PERFECT, heard("l a double you y e r", "LAWYER").verdict)
        assertEquals(Verdict.PERFECT, heard("w a i t e r", "WAITER").verdict)
        assertEquals(Verdict.PERFECT, heard("double u a i t e r", "WAITER").verdict)
    }

    @Test fun lettersInWrongOrderAreWrong() {
        val r = heard("c a e r f u l", "CAREFUL")
        assertTrue(r.distance >= 1)
        assertTrue(r.verdict != Verdict.PERFECT)
    }

    @Test fun everyWordIsPerfectWhenSpelledInBothFormats() {
        for (w in Words.all) {
            val normal = w.text.map { it.lowercase() }.joinToString(" ")
            assertEquals(w.text, Verdict.PERFECT, heard(normal, w.text).verdict)
            val dbl = SpellPlanner.spellText(w.text, true).lowercase().replace(",", " ")
            assertEquals(w.text + " doble", Verdict.PERFECT, heard(dbl, w.text).verdict)
        }
    }

    @Test fun stripWordEndsRemovesOptionalWords() {
        assertEquals(listOf("c", "a"), SpellingEvaluator.stripWordEnds(listOf("cat", "c", "a", "cat"), "CAT"))
        assertEquals(listOf("c", "a"), SpellingEvaluator.stripWordEnds(listOf("c", "a"), "CAT"))
        assertEquals(listOf("c", "a"), SpellingEvaluator.stripWordEnds(listOf("cat", "c", "a"), "CAT"))
        assertEquals(emptyList<String>(), SpellingEvaluator.stripWordEnds(listOf("cat"), "CAT"))
    }
}
