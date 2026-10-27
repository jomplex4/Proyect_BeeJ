import com.digitalminds.beej.core.*
import java.io.File

fun parseTokens(s: String): List<TimedToken> =
    if (s.isBlank()) emptyList() else s.split(";").map { p ->
        val f = p.split(":"); TimedToken(f[0], f[1].toFloat(), f[2].toDouble(), f[3].toDouble())
    }

fun main(args: Array<String>) {
    when (args[0]) {
        "grammar" -> {
            val out = File(args[1]).printWriter()
            for (w in Words.all) out.println(w.text + "\t" + PhraseGrammar.toJson(PhraseGrammar.training(w.text)) + "\t" + PhraseGrammar.toJson(PhraseGrammar.contest(w.text)))
            out.close()
        }
        "judge" -> {
            val out = File(args[2]).printWriter()
            for (line in File(args[1]).readLines()) {
                val f = line.split("|")
                val r = VoiceJudge.judge(f[0], parseTokens(f[2]), parseTokens(f[3]), f[1] == "1")
                out.println(listOf(r.outcome, r.failure, r.spelling.heard, r.doubtLetters.joinToString(",") { "${it.first}${it.second}" }, "%.1f".format(r.lettersPerSec), "%.1f".format(r.maxPauseSec)).joinToString("|"))
            }
            out.close()
        }
    }
}
