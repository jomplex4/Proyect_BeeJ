package com.digitalminds.beej.core

/** Palabra reconocida con su confianza (1.0 si el motor no la informa). */
data class HeardToken(val text: String, val conf: Float = 1f)

enum class ContestFailure { NONE, NOTHING_HEARD, MISSING_START_WORD, MISSING_END_WORD, WRONG_SPELLING }

data class ContestResult(
    val word: String,
    val correct: Boolean,
    val startWordOk: Boolean,
    val endWordOk: Boolean,
    val spelling: SpellingResult,
    val failure: ContestFailure,
    /** true si el error pudo deberse a una palabra que el motor oyó con poca seguridad. */
    val uncertain: Boolean,
    val heardText: String
)

object ContestEvaluator {
    const val LOW_CONFIDENCE = 0.55f

    /** Formato obligatorio: PALABRA, deletreo, PALABRA. */
    fun evaluate(word: String, heard: List<HeardToken>): ContestResult {
        val target = word.lowercase()
        val toks = heard.filter { it.text.isNotBlank() }
        val heardText = toks.joinToString(" ") { it.text }
        if (toks.isEmpty()) {
            return ContestResult(word.uppercase(), false, false, false,
                SpellingEvaluator.evaluate(word, ""), ContestFailure.NOTHING_HEARD, false, "")
        }
        val startOk = toks.first().text == target
        val endOk = toks.size > 1 && toks.last().text == target
        var from = 0
        var to = toks.size
        if (startOk) from = 1
        if (endOk) to = toks.size - 1
        val middle = if (from <= to) toks.subList(from, to) else emptyList()
        // Una palabra del banco dicha en medio del deletreo se cuenta como letra no entendida.
        val middleWords = middle.map { if (it.text in Words.spokenForms) "?" else it.text }
        val spelling = SpellingEvaluator.evaluateTokens(word, middleWords)
        val correct = startOk && endOk && spelling.exact
        val failure = when {
            correct -> ContestFailure.NONE
            !spelling.exact -> ContestFailure.WRONG_SPELLING
            !startOk -> ContestFailure.MISSING_START_WORD
            else -> ContestFailure.MISSING_END_WORD
        }
        val minConf = toks.minOf { it.conf }
        val uncertain = !correct && minConf < LOW_CONFIDENCE
        return ContestResult(word.uppercase(), correct, startOk, endOk, spelling, failure, uncertain, heardText)
    }

    /** Atajo para pruebas y texto plano: "careful c a r e f u l careful". */
    fun evaluateText(word: String, text: String): ContestResult =
        evaluate(word, SpokenLetters.tokenize(text).map { HeardToken(it) })
}
