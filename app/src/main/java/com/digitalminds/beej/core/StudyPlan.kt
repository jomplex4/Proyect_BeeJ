package com.digitalminds.beej.core

data class DayPlan(
    val day: Int,
    val newWords: List<String>,
    val reviewWords: List<String>,
    val simulationRounds: Int,
    val phase: Phase
) {
    /** Todas las palabras que el método recomienda hoy, sin repetir y en orden. */
    val focus: List<String> get() = (newWords + reviewWords).distinct()
}

enum class Phase { LEARN, CONSOLIDATE, COMPETE }

/** Plan intensivo de 14 días para las 30 palabras. */
object StudyPlan {
    const val DAYS = 14

    /** Palabras de la más fácil (corta) a la más difícil (larga); estable respecto a la hoja oficial. */
    val byDifficulty: List<String> =
        Words.all.map { it.text }.withIndex().sortedWith(compareBy({ it.value.length }, { it.index })).map { it.value }

    /** Cinco bloques de seis palabras. */
    val blocks: List<List<String>> = byDifficulty.chunked(6)

    val hardWords: List<String> = byDifficulty.filter { it.length >= 10 }

    fun clampDay(day: Int): Int = day.coerceIn(1, DAYS)

    fun dayFor(startEpochDay: Long, todayEpochDay: Long): Int =
        clampDay((todayEpochDay - startEpochDay).toInt() + 1)

    /**
     * @param errorWords palabras que hoy están en la lista de errores (se priorizan).
     */
    fun forDay(dayIn: Int, errorWords: List<String>): DayPlan {
        val day = clampDay(dayIn)
        val errs = errorWords.distinct()
        return when (day) {
            in 1..5 -> {
                val newW = blocks[day - 1]
                val prev = if (day > 1) blocks[day - 2] else emptyList()
                DayPlan(day, newW, (errs + prev).distinct().filter { it !in newW }, 0, Phase.LEARN)
            }
            in 6..10 -> {
                val block = blocks[day - 6]
                val hard = hardWords.drop((day - 6) * 2).take(2)
                DayPlan(day, emptyList(), (errs + block + hard).distinct(), 3, Phase.CONSOLIDATE)
            }
            else -> {
                val all = if (day >= 13) byDifficulty else (errs + hardWords).distinct()
                val rounds = when (day) { 11 -> 5; 12 -> 6; 13 -> 8; else -> 10 }
                DayPlan(day, emptyList(), (errs + all).distinct(), rounds, Phase.COMPETE)
            }
        }
    }
}
