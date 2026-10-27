package com.digitalminds.beej.core

/**
 * Gramáticas de frases para el reconocedor, hechas a la medida de UNA palabra.
 * Idea: como ya sabemos qué palabra debe deletrear el alumno, el reconocedor solo compara contra
 * el deletreo correcto y contra los errores más probables (letras parecidas, letra omitida,
 * repetida o cambiada de lugar). Así entiende mucho mejor que con letras sueltas.
 */
object PhraseGrammar {
    /** Cómo suenan las letras cuando el reconocedor las escribe como palabra. */
    val NAMES: Map<Char, String> = mapOf(
        'A' to "ay", 'B' to "bee", 'C' to "see", 'D' to "dee", 'E' to "e", 'F' to "ef", 'G' to "gee",
        'H' to "h", 'I' to "i", 'J' to "jay", 'K' to "kay", 'L' to "el", 'M' to "em", 'N' to "en",
        'O' to "oh", 'P' to "pee", 'Q' to "queue", 'R' to "are", 'S' to "es", 'T' to "tea", 'U' to "you",
        'V' to "vee", 'W' to "w", 'X' to "x", 'Y' to "why", 'Z' to "zee"
    )

    /** Letras que se confunden entre sí (por el oído o por el acento). */
    val CONFUSION_CLASSES: List<String> = listOf("BCDEGPTVZ", "AJKH", "MNLSFX", "IYAE", "QUW", "ORL")

    const val UNK = "[unk]"

    fun near(c: Char): List<Char> {
        val out = LinkedHashSet<Char>()
        for (k in CONFUSION_CLASSES) if (c in k) k.forEach { out.add(it) }
        out.remove(c)
        return out.toList()
    }

    /** Errores de deletreo más probables de una palabra (sin incluir la palabra correcta). */
    fun variants(word: String): Set<String> {
        val w = word.uppercase()
        val out = LinkedHashSet<String>()
        for (i in w.indices) {
            val c = w[i]
            for (d in near(c)) out.add(w.substring(0, i) + d + w.substring(i + 1))
            out.add(w.removeRange(i, i + 1))
            out.add(w.substring(0, i) + c + w.substring(i))
            if (i < w.length - 1 && w[i] != w[i + 1]) {
                out.add(w.substring(0, i) + w[i + 1] + w[i] + w.substring(i + 2))
            }
        }
        out.remove(w)
        out.remove("")
        return out
    }

    private fun tokensOf(c: Char, nameForm: Boolean): List<List<String>> {
        val base = if (nameForm) NAMES.getValue(c) else c.lowercase()
        return if (c == 'W') listOf(listOf(base), listOf("double", "you")) else listOf(listOf(base))
    }

    /**
     * Frases (como texto de palabras) que representan un deletreo, en forma "letra" (c a r) y en
     * forma "nombre" (see ay are). Las letras repetidas también pueden decirse "double f".
     */
    fun spokenForms(spelling: String, maxPerForm: Int = 6): List<String> {
        val out = LinkedHashSet<String>()
        for (nameForm in listOf(false, true)) {
            var alts: List<List<String>> = listOf(emptyList())
            var i = 0
            while (i < spelling.length) {
                val c = spelling[i]
                var run = 1
                while (i + run < spelling.length && spelling[i + run] == c) run++
                // alternativas para la corrida: plana y, si hay 2 o más, "double c"
                val runAlts = ArrayList<List<String>>()
                for (t in tokensOf(c, nameForm)) runAlts.add((1..run).flatMap { t })
                if (run >= 2) {
                    for (t in tokensOf(c, nameForm)) runAlts.add(listOf("double") + t + (3..run).flatMap { t })
                }
                val next = ArrayList<List<String>>()
                for (a in alts) for (r in runAlts) {
                    if (next.size >= maxPerForm) break
                    next.add(a + r)
                }
                alts = next
                i += run
            }
            alts.forEach { out.add(it.joinToString(" ")) }
        }
        return out.toList()
    }

    /** Entrenamiento oral: el deletreo correcto y sus errores probables. */
    fun training(word: String): List<String> {
        val w = word.uppercase()
        val out = LinkedHashSet<String>()
        out.addAll(spokenForms(w))
        for (v in variants(w)) out.addAll(spokenForms(v, maxPerForm = 2))
        out.add(UNK)
        return out.toList()
    }

    /** Concurso: PALABRA deletreo PALABRA, más las formas incompletas y los errores de letra. */
    fun contest(word: String): List<String> {
        val w = word.uppercase()
        val lower = w.lowercase()
        val out = LinkedHashSet<String>()
        val correct = spokenForms(w)
        for (s in correct) {
            out.add("$lower $s $lower")
            out.add("$s $lower")      // falta la palabra inicial
            out.add("$lower $s")      // falta la palabra final
            out.add(s)                // faltan ambas
        }
        for (v in variants(w)) for (s in spokenForms(v, maxPerForm = 2)) out.add("$lower $s $lower")
        out.add(UNK)
        return out.toList()
    }

    /** Gramática libre (cualquier letra, "double", las 30 palabras): sirve de segunda opinión. */
    fun free(): List<String> = SpokenLetters.grammarTokens() + UNK

    fun toJson(phrases: List<String>): String {
        // Las frases solo tienen letras minúsculas, espacios y "[unk]": no hace falta escapar nada.
        return phrases.joinToString(",", "[", "]") { "\"" + it + "\"" }
    }
}
