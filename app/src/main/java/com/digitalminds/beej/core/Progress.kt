package com.digitalminds.beej.core

data class WordStats(
    val attempts: Int = 0,
    val correct: Int = 0,
    val errors: Int = 0,
    /** Aciertos seguidos actuales. */
    val streak: Int = 0,
    val lastCorrectDay: Long = -1,
    val distinctCorrectDays: Int = 0,
    val inErrorBook: Boolean = false,
    /** Aciertos desde el último error. */
    val correctSinceError: Int = 0,
    /** Días distintos con acierto desde el último error. */
    val daysSinceError: Int = 0
)

data class Progress(
    val stats: Map<String, WordStats> = emptyMap(),
    val startEpochDay: Long = -1,
    val lastPracticeDay: Long = -1,
    val practiceStreakDays: Int = 0,
    val bestPracticeStreakDays: Int = 0
)

/** Reglas puras de progreso, sin depender de Android. */
object ProgressLogic {
    /** Para salir de la lista de errores: 3 aciertos desde el último error, en al menos 2 días distintos. */
    const val CORRECT_TO_CLEAR = 3
    const val DAYS_TO_CLEAR = 2
    const val HARD_AFTER_ERRORS = 2

    fun statsOf(p: Progress, word: String): WordStats = p.stats[word.uppercase()] ?: WordStats()

    fun touchDay(p: Progress, today: Long): Progress {
        if (p.lastPracticeDay == today) return p
        val streak = if (p.lastPracticeDay == today - 1) p.practiceStreakDays + 1 else 1
        return p.copy(
            startEpochDay = if (p.startEpochDay < 0) today else p.startEpochDay,
            lastPracticeDay = today,
            practiceStreakDays = streak,
            bestPracticeStreakDays = maxOf(p.bestPracticeStreakDays, streak)
        )
    }

    fun record(pIn: Progress, word: String, correct: Boolean, today: Long): Progress {
        val key = word.uppercase()
        val p = touchDay(pIn, today)
        val s = statsOf(p, key)
        val n = if (correct) {
            val newDay = s.lastCorrectDay != today
            var cse = s.correctSinceError
            var dse = s.daysSinceError
            var inBook = s.inErrorBook
            if (inBook) {
                cse += 1
                if (newDay) dse += 1
                if (cse >= CORRECT_TO_CLEAR && dse >= DAYS_TO_CLEAR) {
                    inBook = false; cse = 0; dse = 0
                }
            }
            s.copy(
                attempts = s.attempts + 1,
                correct = s.correct + 1,
                streak = s.streak + 1,
                lastCorrectDay = today,
                distinctCorrectDays = s.distinctCorrectDays + if (newDay) 1 else 0,
                inErrorBook = inBook,
                correctSinceError = cse,
                daysSinceError = dse
            )
        } else {
            s.copy(
                attempts = s.attempts + 1,
                errors = s.errors + 1,
                streak = 0,
                inErrorBook = true,
                correctSinceError = 0,
                daysSinceError = 0
            )
        }
        return p.copy(stats = p.stats + (key to n))
    }

    fun errorWords(p: Progress): List<String> =
        Words.all.map { it.text }.filter { statsOf(p, it).inErrorBook }

    /** Dominada: 3 aciertos seguidos, en 2 o más días distintos y fuera de la lista de errores. */
    fun isMastered(p: Progress, word: String): Boolean {
        val s = statsOf(p, word)
        return !s.inErrorBook && s.streak >= 3 && s.distinctCorrectDays >= 2
    }

    fun masteredWords(p: Progress): List<String> = Words.all.map { it.text }.filter { isMastered(p, it) }

    fun isHard(p: Progress, word: String): Boolean = statsOf(p, word).errors >= HARD_AFTER_ERRORS

    fun hardWords(p: Progress): List<String> = Words.all.map { it.text }.filter { isHard(p, it) }

    fun totalCorrect(p: Progress): Int = p.stats.values.sumOf { it.correct }
    fun totalErrors(p: Progress): Int = p.stats.values.sumOf { it.errors }
    fun totalAttempts(p: Progress): Int = p.stats.values.sumOf { it.attempts }
}

/** Guardado en texto simple (una línea por palabra), fácil de probar. */
object ProgressCodec {
    fun encode(p: Progress): String {
        val sb = StringBuilder()
        sb.append("H|${p.startEpochDay}|${p.lastPracticeDay}|${p.practiceStreakDays}|${p.bestPracticeStreakDays}\n")
        for ((w, s) in p.stats) {
            sb.append("W|$w|${s.attempts}|${s.correct}|${s.errors}|${s.streak}|${s.lastCorrectDay}|")
                .append("${s.distinctCorrectDays}|${if (s.inErrorBook) 1 else 0}|${s.correctSinceError}|${s.daysSinceError}\n")
        }
        return sb.toString()
    }

    fun decode(text: String?): Progress {
        if (text.isNullOrBlank()) return Progress()
        var p = Progress()
        val stats = HashMap<String, WordStats>()
        for (line in text.lineSequence()) {
            val f = line.split('|')
            try {
                when (f.firstOrNull()) {
                    "H" -> p = p.copy(
                        startEpochDay = f[1].toLong(), lastPracticeDay = f[2].toLong(),
                        practiceStreakDays = f[3].toInt(), bestPracticeStreakDays = f[4].toInt()
                    )
                    "W" -> if (Words.find(f[1]) != null) stats[f[1].uppercase()] = WordStats(
                        attempts = f[2].toInt(), correct = f[3].toInt(), errors = f[4].toInt(),
                        streak = f[5].toInt(), lastCorrectDay = f[6].toLong(),
                        distinctCorrectDays = f[7].toInt(), inErrorBook = f[8] == "1",
                        correctSinceError = f[9].toInt(), daysSinceError = f[10].toInt()
                    )
                }
            } catch (e: Exception) {
                // Línea dañada: se ignora, el resto del progreso se conserva.
            }
        }
        return p.copy(stats = stats)
    }
}
