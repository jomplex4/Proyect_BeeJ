package com.digitalminds.beej.core

/** Palabra reconocida con su confianza y sus tiempos (en segundos desde que empezó la grabación). */
data class TimedToken(val text: String, val conf: Float = 1f, val start: Double = 0.0, val end: Double = 0.0)

enum class JudgeOutcome {
    /** Deletreo (y, en el concurso, palabra inicial y final) confirmados. */
    CORRECT,
    /** Error claro: letra cambiada, faltante o sobrante, o falta una de las palabras. */
    WRONG,
    /** No se pudo confirmar: se pide repetir y NO se cuenta como error ni como acierto. */
    UNSURE,
    /** No se oyó nada útil. */
    NOTHING
}

data class VoiceJudgement(
    val outcome: JudgeOutcome,
    val word: String,
    val startWordOk: Boolean,
    val endWordOk: Boolean,
    val spelling: SpellingResult,
    val failure: ContestFailure,
    /** Letras donde la segunda lectura contradijo la primera: (esperada, oída). */
    val doubtLetters: List<Pair<Char, Char>>,
    val maxPauseSec: Double,
    val lettersPerSec: Double,
    val heardText: String
) {
    val longPause: Boolean get() = maxPauseSec > LONG_PAUSE_SEC

    companion object {
        const val LONG_PAUSE_SEC = 1.5
    }
}

/**
 * Decide si un deletreo hablado es correcto combinando DOS lecturas del mismo audio:
 *  1. [forced]: lectura con la gramática de esa palabra (entiende mucho mejor, pero tiende a "adivinar").
 *  2. [free]: lectura libre, que actúa de veto si contradice una letra (por ejemplo oye una J donde va una G).
 * Es estricto a propósito: ante la duda pide repetir en vez de dar por buena una letra distinta.
 */
object VoiceJudge {

    private fun isUnknown(t: String) = t == PhraseGrammar.UNK || t.isBlank()

    fun judge(word: String, forced: List<TimedToken>, free: List<TimedToken>, requireFrame: Boolean): VoiceJudgement {
        val target = word.uppercase()
        val low = target.lowercase()
        val usable = forced.filter { it.text.isNotBlank() }
        val heardText = usable.joinToString(" ") { it.text }
        if (usable.isEmpty() || usable.all { isUnknown(it.text) }) {
            return VoiceJudgement(JudgeOutcome.NOTHING, target, false, false, SpellingEvaluator.evaluate(target, ""),
                ContestFailure.NOTHING_HEARD, emptyList(), 0.0, 0.0, heardText)
        }

        // Primera lectura: estructura y deletreo.
        val startOk: Boolean
        val endOk: Boolean
        val spelling: SpellingResult
        val failure: ContestFailure
        if (requireFrame) {
            val r = ContestEvaluator.evaluate(target, usable.map { HeardToken(it.text, it.conf) })
            startOk = r.startWordOk; endOk = r.endWordOk; spelling = r.spelling; failure = r.failure
        } else {
            val texts = SpellingEvaluator.stripWordEnds(usable.map { it.text }, low)
            spelling = SpellingEvaluator.evaluateTokens(target, texts)
            startOk = true; endOk = true
            failure = if (spelling.exact) ContestFailure.NONE else ContestFailure.WRONG_SPELLING
        }
        val (maxPause, rate) = fluency(usable, low)
        val firstOk = failure == ContestFailure.NONE

        val freeTexts = SpellingEvaluator.stripWordEnds(free.map { it.text }, low)
        val second = SpellingEvaluator.evaluateTokens(target, freeTexts)

        if (firstOk) {
            // Veto (estricto a propósito): si la segunda lectura oyó otra letra conocida en alguna posición,
            // no se da por correcto; se pide repetir. Un falso "correcto" es peor que repetir.
            val doubt = second.steps.firstOrNull {
                it.op == Op.SUB && it.heard != null && it.heard != '?' && it.expected != null
            }
            val vetoed = doubt != null
            return if (!vetoed) {
                VoiceJudgement(JudgeOutcome.CORRECT, target, true, true, spelling, failure, emptyList(), maxPause, rate, heardText)
            } else {
                VoiceJudgement(JudgeOutcome.UNSURE, target, true, true, spelling, failure,
                    listOf(doubt!!.expected!! to doubt.heard!!), maxPause, rate, heardText)
            }
        }

        val frameOk = !requireFrame || (startOk && endOk)
        if (frameOk) {
            // Error de deletreo: debe confirmarlo la segunda lectura en la misma posición (o junto a ella).
            val idx = spelling.firstErrorIndex
            // La segunda lectura confirma el error solo si oyó OTRA letra conocida (o ninguna) en esa posición.
            val confirmed = HashSet<Int>()
            var pos = 0
            for (st in second.steps) {
                if (st.op == Op.EXTRA) { confirmed.add(pos); continue }
                if (st.op == Op.MISSING || (st.op == Op.SUB && st.heard != '?')) confirmed.add(pos)
                pos++
            }
            val corroborated = idx >= 0 && (idx in confirmed || (idx - 1) in confirmed || (idx + 1) in confirmed)
            // Si lo único que falla son palabras que el reconocedor no pudo clasificar, tampoco es error del alumno.
            val onlyUnknown = spelling.steps.none { it.op == Op.MISSING || it.op == Op.EXTRA } &&
                spelling.steps.filter { it.op == Op.SUB }.all { it.heard == '?' }
            val outcome = if (onlyUnknown || !corroborated) JudgeOutcome.UNSURE else JudgeOutcome.WRONG
            return VoiceJudgement(outcome, target, startOk, endOk, spelling, failure, emptyList(), maxPause, rate, heardText)
        }
        // Falta la palabra inicial o final: eso lo detecta bien el reconocedor, es un error claro.
        return VoiceJudgement(JudgeOutcome.WRONG, target, startOk, endOk, spelling, failure, emptyList(), maxPause, rate, heardText)
    }

    /** Pausa más larga entre letras y ritmo (letras por segundo) dentro de la parte deletreada. */
    private fun fluency(tokens: List<TimedToken>, wordLower: String): Pair<Double, Double> {
        var list = tokens
        if (list.isNotEmpty() && list.first().text == wordLower) list = list.drop(1)
        if (list.isNotEmpty() && list.last().text == wordLower) list = list.dropLast(1)
        val letters = list.filter { it.text != "double" && !isUnknown(it.text) && it.end > it.start }
        if (letters.size < 2) return 0.0 to 0.0
        var maxGap = 0.0
        for (i in 1 until letters.size) maxGap = maxOf(maxGap, letters[i].start - letters[i - 1].end)
        val span = letters.last().end - letters.first().start
        val rate = if (span > 0) letters.size / span else 0.0
        return maxGap to rate
    }
}
