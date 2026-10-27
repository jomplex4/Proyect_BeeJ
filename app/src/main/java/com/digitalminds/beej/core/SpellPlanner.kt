package com.digitalminds.beej.core

enum class Speed(val suffix: String, val label: String) {
    SLOW("s", "Lento"), NORMAL("n", "Normal"), FAST("f", "Rápido");

    companion object {
        fun fromOrdinal(i: Int): Speed = values().getOrElse(i) { NORMAL }
    }
}

/**
 * Niveles de velocidad del deletreo. No hay nivel lento: el más tranquilo es "Normal".
 * [gapMs] es la pausa tras cada letra; el último nivel es el ritmo de los campeonatos (unas 3 letras por segundo).
 */
enum class SpellSpeed(val suffix: String, val label: String, val gapMs: Int, val doubleGapMs: Int) {
    NORMAL("n", "Normal", 650, 130),
    FAST("f", "Rápido", 330, 90),
    VERY_FAST("v", "Muy rápido", 160, 50),
    EXTRA_FAST("x", "Extra rápido", 60, 20);

    companion object {
        fun fromOrdinal(i: Int): SpellSpeed = values().getOrElse(i) { NORMAL }
    }
}

/** Un audio dentro de una secuencia, con la pausa que va después. */
data class AudioStep(val clip: String, val gapAfterMs: Int)

/**
 * Arma la secuencia de audios para deletrear una palabra.
 * Cada letra es un audio independiente, así controlamos la pausa entre letras.
 */
object SpellPlanner {
    /** Pausa entre el final de una letra y el inicio de la siguiente. */
    fun letterGapMs(speed: SpellSpeed): Int = speed.gapMs

    fun letterClip(letter: Char): String = "l_" + letter.lowercaseChar()
    const val DOUBLE_CLIP = "double"

    /**
     * @param doubleMode true: "DOUBLE F" para letras repetidas seguidas. false: "F, F".
     */
    fun spellSteps(word: String, doubleMode: Boolean, speed: SpellSpeed): List<AudioStep> {
        val gap = letterGapMs(speed)
        val steps = ArrayList<AudioStep>()
        val w = word.uppercase()
        var i = 0
        while (i < w.length) {
            val c = w[i]
            var run = 1
            while (i + run < w.length && w[i + run] == c) run++
            var left = run
            while (left > 0) {
                if (doubleMode && left >= 2) {
                    steps.add(AudioStep(DOUBLE_CLIP, speed.doubleGapMs))
                    steps.add(AudioStep(letterClip(c), gap))
                    left -= 2
                } else {
                    steps.add(AudioStep(letterClip(c), gap))
                    left -= 1
                }
            }
            i += run
        }
        return steps
    }

    /** Lo que se verá/leerá como guion de la secuencia, por ejemplo "C, O, DOUBLE F, E, E". */
    fun spellText(word: String, doubleMode: Boolean): String =
        spellSteps(word, doubleMode, SpellSpeed.NORMAL).let { steps ->
            val out = ArrayList<String>()
            var pendingDouble = false
            for (s in steps) {
                if (s.clip == DOUBLE_CLIP) { pendingDouble = true; continue }
                val l = s.clip.removePrefix("l_").uppercase()
                out.add(if (pendingDouble) "DOUBLE $l" else l)
                pendingDouble = false
            }
            out.joinToString(", ")
        }

    /**
     * Para cada paso de [spellSteps], qué letras de la palabra se están diciendo (para resaltarlas).
     * El paso "double" resalta las dos letras; el paso de la letra siguiente repite ese resaltado.
     */
    fun letterSpans(word: String, doubleMode: Boolean): List<IntRange> {
        val w = word.uppercase()
        val spans = ArrayList<IntRange>()
        var i = 0
        while (i < w.length) {
            val c = w[i]
            var run = 1
            while (i + run < w.length && w[i + run] == c) run++
            var left = run
            var pos = i
            while (left > 0) {
                if (doubleMode && left >= 2) {
                    spans.add(pos..pos + 1); spans.add(pos..pos + 1)
                    pos += 2; left -= 2
                } else {
                    spans.add(pos..pos); pos += 1; left -= 1
                }
            }
            i += run
        }
        return spans
    }

    fun wordClip(word: String): String = "w_" + word.lowercase()

    /** Archivo de una palabra según su velocidad. */
    fun fileFor(clip: String, speed: Speed): String = "${clip}_${speed.suffix}"

    /** Archivo de una letra (o de "double") según la velocidad del deletreo. */
    fun letterFile(clip: String, speed: SpellSpeed): String = "${clip}_${speed.suffix}"
}
