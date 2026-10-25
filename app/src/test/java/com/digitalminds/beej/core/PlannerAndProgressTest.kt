package com.digitalminds.beej.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerAndProgressTest {
    @Test fun coffeeNormalSequence() {
        val steps = SpellPlanner.spellSteps("COFFEE", false, Speed.NORMAL)
        assertEquals(listOf("l_c", "l_o", "l_f", "l_f", "l_e", "l_e"), steps.map { it.clip })
        assertEquals("C, O, F, F, E, E", SpellPlanner.spellText("COFFEE", false))
    }

    @Test fun coffeeDoubleSequence() {
        val steps = SpellPlanner.spellSteps("COFFEE", true, Speed.NORMAL)
        assertEquals(listOf("l_c", "l_o", "double", "l_f", "double", "l_e"), steps.map { it.clip })
        assertEquals("C, O, DOUBLE F, DOUBLE E", SpellPlanner.spellText("COFFEE", true))
    }

    @Test fun doubleAppliesToAnyRepeatedLetter() {
        assertEquals("R, E, A, DOUBLE L, Y", SpellPlanner.spellText("REALLY", true))
        assertEquals("R, E, A, L, L, Y", SpellPlanner.spellText("REALLY", false))
        assertEquals("P, A, P, A, R, A, DOUBLE Z, O", SpellPlanner.spellText("PAPARAZZO", true))
        assertEquals("C, O, DOUBLE M, U, N, I, T, Y", SpellPlanner.spellText("COMMUNITY", true))
    }

    @Test fun nonConsecutiveRepeatsAreNotDoubled() {
        assertEquals("P, A, P, A", SpellPlanner.spellText("PAPA", true))
        assertEquals("L, A, W, Y, E, R", SpellPlanner.spellText("LAWYER", true))
    }

    @Test fun letterGapsGrowWithSlowerSpeedAndNeverTooTight() {
        val s = SpellPlanner.letterGapMs(Speed.SLOW)
        val n = SpellPlanner.letterGapMs(Speed.NORMAL)
        val f = SpellPlanner.letterGapMs(Speed.FAST)
        assertTrue(s > n && n > f)
        assertTrue(f >= 400)
        assertTrue(SpellPlanner.spellSteps("PILOT", false, Speed.FAST).all { it.gapAfterMs == f })
    }

    @Test fun everyWordUsesOnlyKnownClips() {
        val known = ('a'..'z').map { "l_$it" }.toSet() + "double"
        for (w in Words.all) for (dbl in listOf(false, true)) {
            assertTrue(w.text, SpellPlanner.spellSteps(w.text, dbl, Speed.NORMAL).all { it.clip in known })
        }
    }

    @Test fun spansMatchStepsInBothModes() {
        for (w in Words.all) for (dbl in listOf(false, true)) {
            assertEquals(w.text, SpellPlanner.spellSteps(w.text, dbl, Speed.NORMAL).size, SpellPlanner.letterSpans(w.text, dbl).size)
        }
        assertEquals(listOf(0..0, 1..1, 2..3, 2..3, 4..5, 4..5), SpellPlanner.letterSpans("COFFEE", true))
        assertEquals(6, SpellPlanner.letterSpans("COFFEE", false).size)
    }

    @Test fun planCoversAllThirtyWordsInFiveBlocks() {
        assertEquals(5, StudyPlan.blocks.size)
        assertEquals(30, StudyPlan.blocks.flatten().toSet().size)
        val learned = (1..5).flatMap { StudyPlan.forDay(it, emptyList()).newWords }
        assertEquals(30, learned.toSet().size)
    }

    @Test fun planEndsWithEveryWordAndSimulations() {
        val d14 = StudyPlan.forDay(14, emptyList())
        assertEquals(30, d14.focus.size)
        assertTrue(d14.simulationRounds >= StudyPlan.forDay(11, emptyList()).simulationRounds)
        assertEquals(Phase.COMPETE, d14.phase)
    }

    @Test fun errorsArePrioritizedInPlan() {
        val d = StudyPlan.forDay(8, listOf("PILOT"))
        assertEquals("PILOT", d.reviewWords.first())
    }

    @Test fun dayIsClampedBetween1And14() {
        assertEquals(1, StudyPlan.dayFor(100, 100))
        assertEquals(3, StudyPlan.dayFor(100, 102))
        assertEquals(14, StudyPlan.dayFor(100, 500))
        assertEquals(1, StudyPlan.dayFor(100, 50))
    }

    @Test fun errorEntersListAndNeedsThreeCorrectOnTwoDays() {
        var p = Progress()
        p = ProgressLogic.record(p, "PILOT", false, 10)
        assertEquals(listOf("PILOT"), ProgressLogic.errorWords(p))
        // tres aciertos el mismo día no bastan
        for (i in 1..3) p = ProgressLogic.record(p, "PILOT", true, 10)
        assertTrue(ProgressLogic.statsOf(p, "PILOT").inErrorBook)
        // un acierto más en otro día lo saca
        p = ProgressLogic.record(p, "PILOT", true, 11)
        assertFalse(ProgressLogic.statsOf(p, "PILOT").inErrorBook)
        assertTrue(ProgressLogic.errorWords(p).isEmpty())
    }

    @Test fun anotherErrorResetsTheCount() {
        var p = Progress()
        p = ProgressLogic.record(p, "SUGAR", false, 1)
        p = ProgressLogic.record(p, "SUGAR", true, 1)
        p = ProgressLogic.record(p, "SUGAR", true, 2)
        p = ProgressLogic.record(p, "SUGAR", false, 2)
        val s = ProgressLogic.statsOf(p, "SUGAR")
        assertEquals(0, s.correctSinceError)
        assertEquals(0, s.streak)
        assertEquals(2, s.errors)
        assertTrue(ProgressLogic.isHard(p, "SUGAR"))
    }

    @Test fun masteryNeedsStreakAndTwoDays() {
        var p = Progress()
        for (i in 1..3) p = ProgressLogic.record(p, "COFFEE", true, 5)
        assertFalse(ProgressLogic.isMastered(p, "COFFEE"))
        p = ProgressLogic.record(p, "COFFEE", true, 6)
        assertTrue(ProgressLogic.isMastered(p, "COFFEE"))
    }

    @Test fun practiceStreakCountsConsecutiveDays() {
        var p = Progress()
        p = ProgressLogic.record(p, "PILOT", true, 1)
        p = ProgressLogic.record(p, "PILOT", true, 2)
        p = ProgressLogic.record(p, "PILOT", true, 3)
        assertEquals(3, p.practiceStreakDays)
        p = ProgressLogic.record(p, "PILOT", true, 5)
        assertEquals(1, p.practiceStreakDays)
        assertEquals(3, p.bestPracticeStreakDays)
        assertEquals(1L, p.startEpochDay)
    }

    @Test fun totalsAddUp() {
        var p = Progress()
        p = ProgressLogic.record(p, "PILOT", true, 1)
        p = ProgressLogic.record(p, "SUGAR", false, 1)
        assertEquals(1, ProgressLogic.totalCorrect(p))
        assertEquals(1, ProgressLogic.totalErrors(p))
        assertEquals(2, ProgressLogic.totalAttempts(p))
    }

    @Test fun codecRoundTrip() {
        var p = Progress()
        p = ProgressLogic.record(p, "PILOT", false, 7)
        p = ProgressLogic.record(p, "PILOT", true, 8)
        p = ProgressLogic.record(p, "CAREFUL", true, 8)
        val back = ProgressCodec.decode(ProgressCodec.encode(p))
        assertEquals(p, back)
    }

    @Test fun codecSurvivesGarbage() {
        assertEquals(Progress(), ProgressCodec.decode(null))
        assertEquals(Progress(), ProgressCodec.decode(""))
        val p = ProgressCodec.decode("basura\nW|PILOT|x|y\nW|CARIFUL|1|1|0|1|1|1|0|0|0\nH|1|2|3|4")
        assertEquals(1L, p.startEpochDay)
        assertTrue(p.stats.isEmpty())
    }
}
