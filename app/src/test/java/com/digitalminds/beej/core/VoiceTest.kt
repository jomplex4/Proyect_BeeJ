package com.digitalminds.beej.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTest {
    private fun toks(text: String, t0: Double = 0.0, step: Double = 0.4, conf: Float = 1f): List<TimedToken> =
        text.split(" ").filter { it.isNotBlank() }.mapIndexed { i, w -> TimedToken(w, conf, t0 + i * step, t0 + i * step + 0.3) }

    // ---- gramática por palabra
    @Test fun correctPhrasesIncludeBothFormsAndDouble() {
        val f = PhraseGrammar.spokenForms("COFFEE")
        assertTrue("c o f f e e" in f)
        assertTrue("see oh double ef double e" in f)
        assertTrue("c o double f e e" in f)
        assertTrue(f.all { it == it.lowercase() })
    }

    @Test fun wHasDoubleYouAlternative() {
        val f = PhraseGrammar.spokenForms("LAWYER")
        assertTrue("l a w y e r" in f)
        assertTrue("l a double you y e r" in f)
    }

    @Test fun variantsCoverTheTypicalErrors() {
        val v = PhraseGrammar.variants("BEACH")
        assertTrue("VEACH" in v)        // B/V
        assertTrue("EACH" in v)         // letra omitida
        assertTrue("BEEACH" in v)       // letra repetida
        assertTrue("EBACH" in v)        // letras cambiadas de lugar
        assertFalse("BEACH" in v)
        assertTrue('V' in PhraseGrammar.near('B') && 'N' in PhraseGrammar.near('M') && 'Z' in PhraseGrammar.near('C') && 'D' in PhraseGrammar.near('T'))
    }

    @Test fun contestGrammarHasStructuresAndStaysSmall() {
        for (w in Words.all) {
            val g = PhraseGrammar.contest(w.text)
            val lower = w.lower
            assertTrue(w.text, g.any { it.startsWith("$lower ") && it.endsWith(" $lower") })
            assertTrue(g.any { !it.startsWith(lower) && it.endsWith(" $lower") })
            assertTrue(g.contains(PhraseGrammar.UNK))
            assertTrue(w.text + " " + g.size, g.size < 900)
        }
    }

    @Test fun grammarJsonIsAValidStringArray() {
        assertEquals("[\"a b\",\"[unk]\"]", PhraseGrammar.toJson(listOf("a b", "[unk]")))
    }

    // ---- juez
    private fun judge(word: String, forced: String, free: String, frame: Boolean = false, conf: Float = 1f) =
        VoiceJudge.judge(word, toks(forced, conf = conf), toks(free), frame)

    @Test fun trainingCorrectConfirmedByBothReadings() {
        val r = judge("CAREFUL", "c a r e f u l", "see ay are e ef you el")
        assertEquals(JudgeOutcome.CORRECT, r.outcome)
    }

    @Test fun freeReadingContradictingALetterMakesItUnsure() {
        val r = judge("RECORDING", "are e see oh are dee i n gee", "are e see oh are dee i n jay")
        assertEquals(JudgeOutcome.UNSURE, r.outcome)
        assertEquals(listOf('G' to 'J'), r.doubtLetters)
    }

    @Test fun clearErrorFromFirstReadingIsWrongAndLocated() {
        val r = judge("CAREFUL", "c a r f u l", "c a r f u l")
        assertEquals(JudgeOutcome.WRONG, r.outcome)
        assertEquals(3, r.spelling.firstErrorIndex)
    }

    @Test fun substitutionDetectedByVariantPhrase() {
        val r = judge("DICTIONARY", "d i c t i o n a l y", "d i c t i o n a l y")
        assertEquals(JudgeOutcome.WRONG, r.outcome)
        assertEquals('R', r.spelling.steps.first { it.op == Op.SUB }.expected)
        assertEquals('L', r.spelling.steps.first { it.op == Op.SUB }.heard)
    }

    @Test fun errorNotConfirmedBySecondReadingIsUnsureNotWrong() {
        val r = judge("DICTIONARY", "d i c t i o n a l y", "d i c t i o n a r y")
        assertEquals(JudgeOutcome.UNSURE, r.outcome)
    }

    @Test fun onlyUnknownTokensIsUnsureNotAnError() {
        val r = judge("PILOT", "p [unk] l o t", "p i l o t")
        assertEquals(JudgeOutcome.UNSURE, r.outcome)
    }

    @Test fun emptyOrUnknownIsNothing() {
        assertEquals(JudgeOutcome.NOTHING, VoiceJudge.judge("PILOT", emptyList(), emptyList(), false).outcome)
        assertEquals(JudgeOutcome.NOTHING, judge("PILOT", "[unk]", "").outcome)
    }

    @Test fun contestNeedsWordSpellingWord() {
        assertEquals(JudgeOutcome.CORRECT, judge("CAREFUL", "careful c a r e f u l careful", "careful c a r e f u l careful", frame = true).outcome)
        val noEnd = judge("CAREFUL", "careful c a r e f u l", "careful c a r e f u l", frame = true)
        assertEquals(JudgeOutcome.WRONG, noEnd.outcome)
        assertEquals(ContestFailure.MISSING_END_WORD, noEnd.failure)
        val noStart = judge("CAREFUL", "c a r e f u l careful", "c a r e f u l careful", frame = true)
        assertEquals(ContestFailure.MISSING_START_WORD, noStart.failure)
        val none = judge("CAREFUL", "c a r e f u l", "c a r e f u l", frame = true)
        assertEquals(JudgeOutcome.WRONG, none.outcome)
        assertFalse(none.startWordOk || none.endWordOk)
    }

    @Test fun contestDoubleAndRepeatedLetters() {
        assertEquals(JudgeOutcome.CORRECT, judge("COFFEE", "coffee c o double f e e coffee", "coffee c o double f e e coffee", frame = true).outcome)
        assertEquals(JudgeOutcome.CORRECT, judge("REALLY", "really r e a l l y really", "really r e a l l y really", frame = true).outcome)
        assertEquals(JudgeOutcome.WRONG, judge("REALLY", "really r e double a l y really", "really r e a l y really", frame = true).outcome)
    }

    @Test fun contestWrongLetterStillShowsWhere() {
        val r = judge("PILOT", "pilot p a l o t pilot", "pilot p a l o t pilot", frame = true)
        assertEquals(JudgeOutcome.WRONG, r.outcome)
        assertEquals(1, r.spelling.firstErrorIndex)
    }

    @Test fun fluencyMeasuresPausesAndRate() {
        val fast = VoiceJudge.judge("PILOT", toks("p i l o t", step = 0.35), toks("p i l o t"), false)
        assertFalse(fast.longPause)
        assertTrue(fast.lettersPerSec > 2.0)
        val slow = listOf(TimedToken("p", 1f, 0.0, 0.3), TimedToken("i", 1f, 2.4, 2.7), TimedToken("l", 1f, 2.9, 3.2),
            TimedToken("o", 1f, 3.4, 3.7), TimedToken("t", 1f, 3.9, 4.2))
        val r = VoiceJudge.judge("PILOT", slow, toks("p i l o t"), false)
        assertTrue(r.longPause)
        assertEquals(JudgeOutcome.CORRECT, r.outcome)
    }

    @Test fun everyWordPassesInBothModesWhenBothReadingsAgree() {
        for (w in Words.all) {
            val spelled = w.lower.toCharArray().joinToString(" ")
            assertEquals(w.text, JudgeOutcome.CORRECT, judge(w.text, spelled, spelled).outcome)
            val full = "${w.lower} $spelled ${w.lower}"
            assertEquals(w.text, JudgeOutcome.CORRECT, judge(w.text, full, full, frame = true).outcome)
        }
    }
}
