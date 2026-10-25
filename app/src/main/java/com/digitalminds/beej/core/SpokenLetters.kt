package com.digitalminds.beej.core

/**
 * Cómo suelen transcribir los reconocedores de voz los nombres de las letras.
 * Todas las formas son una sola palabra en minúsculas. "double u" se trata aparte.
 */
object SpokenLetters {
    const val DOUBLE = "double"

    val variants: Map<Char, List<String>> = mapOf(
        'A' to listOf("a", "ay", "ey"),
        'B' to listOf("b", "bee", "be"),
        'C' to listOf("c", "see", "sea", "cee"),
        'D' to listOf("d", "dee"),
        'E' to listOf("e", "ee"),
        'F' to listOf("f", "ef", "eff"),
        'G' to listOf("g", "gee"),
        'H' to listOf("h", "aitch"),
        'I' to listOf("i", "eye", "aye"),
        'J' to listOf("j", "jay"),
        'K' to listOf("k", "kay"),
        'L' to listOf("l", "el", "ell"),
        'M' to listOf("m", "em"),
        'N' to listOf("n", "en"),
        'O' to listOf("o", "oh", "owe"),
        'P' to listOf("p", "pee", "pea"),
        'Q' to listOf("q", "queue", "cue"),
        'R' to listOf("r", "are", "ar"),
        'S' to listOf("s", "es", "ess"),
        'T' to listOf("t", "tea", "tee"),
        'U' to listOf("u", "you", "yew"),
        'V' to listOf("v", "vee"),
        'W' to listOf("w"),
        'X' to listOf("x", "ex"),
        'Y' to listOf("y", "why", "wye"),
        'Z' to listOf("z", "zee", "zed")
    )

    /** Formas habladas de la W en dos palabras ("double u", "double you"). */
    val doubleUForms: Set<String> = setOf("u", "you", "yew")

    val tokenToLetter: Map<String, Char> =
        variants.flatMap { (letter, forms) -> forms.map { it to letter } }.toMap()

    /** Todas las palabras que le damos a la gramática cerrada del reconocedor. */
    fun grammarTokens(): List<String> =
        (tokenToLetter.keys + DOUBLE + Words.spokenForms).distinct()

    /** Texto crudo -> palabras en minúsculas sin signos. */
    fun tokenize(raw: String): List<String> =
        raw.lowercase()
            .replace(Regex("[^a-z ]"), " ")
            .split(' ')
            .filter { it.isNotBlank() }
}
