package com.digitalminds.beej.core

enum class Op { MATCH, SUB, MISSING, EXTRA }

data class AlignStep(val op: Op, val expected: Char?, val heard: Char?)

enum class Verdict { PERFECT, ALMOST, REINFORCE }

data class SpellingResult(
    val expected: String,
    val heard: String,
    val steps: List<AlignStep>,
    val distance: Int,
    val verdict: Verdict,
    /** Posición (desde 0) de la primera letra esperada con problema, o -1. */
    val firstErrorIndex: Int
) {
    val exact: Boolean get() = distance == 0
}

object SpellingEvaluator {

    /** Convierte la lista de palabras reconocidas en posibles deletreos (letras en mayúscula). '?' = no entendido. */
    fun candidates(tokens: List<String>): List<String> {
        var results = listOf("")
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val next = tokens.getOrNull(i + 1)
            val options: List<Pair<String, Int>> // (letras que se agregan, tokens consumidos)
            if (t == SpokenLetters.DOUBLE) {
                val nextLetter = next?.let { SpokenLetters.tokenToLetter[it] }
                options = if (nextLetter == null) {
                    listOf("?" to 1)
                } else if (next in SpokenLetters.doubleUForms) {
                    // "double u" puede ser la letra W o dos U seguidas.
                    listOf("W" to 2, "UU" to 2)
                } else {
                    listOf("$nextLetter$nextLetter" to 2)
                }
            } else {
                val letter = SpokenLetters.tokenToLetter[t]
                options = listOf((letter?.toString() ?: "?") to 1)
            }
            val consumed = options.first().second
            results = results.flatMap { r -> options.map { r + it.first } }.distinct().take(16)
            i += consumed
        }
        return results
    }

    fun evaluate(expected: String, heard: String): SpellingResult {
        val e = expected.uppercase()
        val h = heard.uppercase()
        val n = e.length
        val m = h.length
        val d = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) d[i][0] = i
        for (j in 0..m) d[0][j] = j
        for (i in 1..n) for (j in 1..m) {
            val cost = if (e[i - 1] == h[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j - 1] + cost, d[i - 1][j] + 1, d[i][j - 1] + 1)
        }
        val steps = ArrayList<AlignStep>()
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && d[i][j] == d[i - 1][j - 1] + (if (e[i - 1] == h[j - 1]) 0 else 1) -> {
                    val same = e[i - 1] == h[j - 1]
                    steps.add(AlignStep(if (same) Op.MATCH else Op.SUB, e[i - 1], h[j - 1]))
                    i--; j--
                }
                i > 0 && d[i][j] == d[i - 1][j] + 1 -> {
                    steps.add(AlignStep(Op.MISSING, e[i - 1], null)); i--
                }
                else -> {
                    steps.add(AlignStep(Op.EXTRA, null, h[j - 1])); j--
                }
            }
        }
        steps.reverse()
        val dist = d[n][m]
        var pos = 0
        var first = -1
        for (s in steps) {
            if (s.op != Op.MATCH && first == -1) first = pos
            if (s.op != Op.EXTRA) pos++
        }
        val verdict = when {
            dist == 0 -> Verdict.PERFECT
            dist == 1 -> Verdict.ALMOST
            else -> Verdict.REINFORCE
        }
        return SpellingResult(e, h, steps, dist, verdict, first)
    }

    /** En el entrenamiento oral el alumno puede decir la palabra antes y después: se ignora. */
    fun stripWordEnds(tokens: List<String>, word: String): List<String> {
        val w = word.lowercase()
        var from = 0
        var to = tokens.size
        if (to > 0 && tokens[0] == w) from = 1
        if (to - 1 >= from && tokens[to - 1] == w) to -= 1
        return tokens.subList(from, to)
    }

    /** Evalúa las palabras reconocidas contra la palabra esperada, eligiendo la mejor interpretación. */
    fun evaluateTokens(expected: String, tokens: List<String>): SpellingResult {
        val cands = candidates(tokens)
        return cands.map { evaluate(expected, it) }.minByOrNull { it.distance }
            ?: evaluate(expected, "")
    }
}
