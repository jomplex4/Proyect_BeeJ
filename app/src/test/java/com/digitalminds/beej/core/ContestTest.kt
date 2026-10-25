package com.digitalminds.beej.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContestTest {
    private fun eval(word: String, text: String) = ContestEvaluator.evaluateText(word, text)

    @Test fun carefulCorrectFormat() {
        val r = eval("CAREFUL", "careful c a r e f u l careful")
        assertTrue(r.correct)
        assertEquals(ContestFailure.NONE, r.failure)
    }

    @Test fun missingWordsAtStartAndEndIsWrong() {
        val r = eval("CAREFUL", "c a r e f u l")
        assertFalse(r.correct)
        assertFalse(r.startWordOk)
        assertFalse(r.endWordOk)
    }

    @Test fun missingLetterIsWrong() {
        val r = eval("CAREFUL", "careful c a r f u l careful")
        assertFalse(r.correct)
        assertEquals(ContestFailure.WRONG_SPELLING, r.failure)
        assertEquals(3, r.spelling.firstErrorIndex)
    }

    @Test fun missingFinalWordIsWrong() {
        val r = eval("CAREFUL", "careful c a r e f u l")
        assertFalse(r.correct)
        assertTrue(r.startWordOk)
        assertFalse(r.endWordOk)
        assertEquals(ContestFailure.MISSING_END_WORD, r.failure)
    }

    @Test fun missingFirstWordIsWrong() {
        val r = eval("CAREFUL", "c a r e f u l careful")
        assertFalse(r.correct)
        assertFalse(r.startWordOk)
        assertTrue(r.endWordOk)
        assertEquals(ContestFailure.MISSING_START_WORD, r.failure)
    }

    @Test fun wrongWordAtTheEndIsWrong() {
        assertFalse(eval("CAREFUL", "careful c a r e f u l pilot").correct)
    }

    @Test fun coffeeNormalMode() {
        assertTrue(eval("COFFEE", "coffee c o f f e e coffee").correct)
    }

    @Test fun coffeeDoubleMode() {
        assertTrue(eval("COFFEE", "coffee c o double f e e coffee").correct)
    }

    @Test fun reallyWithDouble() {
        assertTrue(eval("REALLY", "really r e a double l y really").correct)
        assertTrue(eval("REALLY", "really r e a l l y really").correct)
    }

    @Test fun reallyWithWrongDoubleIsWrong() {
        assertFalse(eval("REALLY", "really r e double a l y really").correct)
    }

    @Test fun nothingHeard() {
        val r = ContestEvaluator.evaluate("PILOT", emptyList())
        assertFalse(r.correct)
        assertEquals(ContestFailure.NOTHING_HEARD, r.failure)
    }

    @Test fun spokenNamesInsideContestFormat() {
        assertTrue(eval("PEOPLE", "people pee e o pee el e people").correct)
    }

    @Test fun lowConfidenceErrorIsMarkedUncertain() {
        val toks = listOf(HeardToken("pilot"), HeardToken("p"), HeardToken("a", 0.3f),
            HeardToken("l"), HeardToken("o"), HeardToken("t"), HeardToken("pilot"))
        val r = ContestEvaluator.evaluate("PILOT", toks)
        assertFalse(r.correct)
        assertTrue(r.uncertain)
    }

    @Test fun confidentErrorIsNotUncertain() {
        val r = ContestEvaluator.evaluate("PILOT", SpokenLetters.tokenize("pilot p a l o t pilot").map { HeardToken(it, 0.95f) })
        assertFalse(r.correct)
        assertFalse(r.uncertain)
    }

    @Test fun correctAnswerIsNeverUncertain() {
        val toks = SpokenLetters.tokenize("pilot p i l o t pilot").map { HeardToken(it, 0.2f) }
        assertFalse(ContestEvaluator.evaluate("PILOT", toks).uncertain)
    }

    @Test fun everyWordPassesFullContestFormat() {
        for (w in Words.all) {
            val text = w.lower + " " + w.text.lowercase().toCharArray().joinToString(" ") + " " + w.lower
            assertTrue(w.text, eval(w.text, text).correct)
        }
    }
}
