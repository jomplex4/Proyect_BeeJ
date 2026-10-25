package com.digitalminds.beej.core

/** Una palabra del banco oficial del concurso (3.º, 4.º y 5.º de secundaria). */
data class WordEntry(val text: String, val es: String) {
    val lower: String get() = text.lowercase()
    /** Nombre base del archivo de audio, por ejemplo "w_careful". */
    val clip: String get() = "w_$lower"
}

object Words {
    /**
     * Banco oficial en el orden de la hoja del colegio.
     * Nota: la hoja impresa dice CARIFUL (error de imprenta). La app usa CAREFUL.
     */
    val all: List<WordEntry> = listOf(
        WordEntry("SINGER", "cantante"),
        WordEntry("LAWYER", "abogado"),
        WordEntry("ALWAYS", "siempre"),
        WordEntry("WAITER", "mesero"),
        WordEntry("REALLY", "realmente"),
        WordEntry("PEOPLE", "gente"),
        WordEntry("DOCTOR", "médico"),
        WordEntry("PILOT", "piloto"),
        WordEntry("SUGAR", "azúcar"),
        WordEntry("COFFEE", "café"),
        WordEntry("MUSICIAN", "músico"),
        WordEntry("BEAUTIFUL", "hermoso"),
        WordEntry("EXCITED", "emocionado"),
        WordEntry("EXPENSIVE", "caro"),
        WordEntry("NORMALLY", "normalmente"),
        WordEntry("POLICEMAN", "policía"),
        WordEntry("RECORDING", "grabación"),
        WordEntry("CAREFUL", "cuidadoso"),
        WordEntry("PINEAPPLE", "piña"),
        WordEntry("DANGEROUS", "peligroso"),
        WordEntry("WATERMELON", "sandía"),
        WordEntry("INTERESTING", "interesante"),
        WordEntry("UNCOUNTABLE", "incontable"),
        WordEntry("PARTICIPATION", "participación"),
        WordEntry("PAPARAZZO", "fotógrafo de farándula"),
        WordEntry("DICTIONARY", "diccionario"),
        WordEntry("PRONUNCIATION", "pronunciación"),
        WordEntry("COMPREHENSION", "comprensión"),
        WordEntry("COMMUNITY", "comunidad"),
        WordEntry("INTELLIGENT", "inteligente")
    )

    fun find(text: String): WordEntry? = all.firstOrNull { it.text.equals(text, ignoreCase = true) }

    /** Palabras que la voz puede decir al principio y al final (en minúsculas). */
    val spokenForms: List<String> get() = all.map { it.lower }
}
