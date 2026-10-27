package com.digitalminds.beej.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalminds.beej.AppViewModel
import com.digitalminds.beej.R
import com.digitalminds.beej.core.JudgeOutcome
import com.digitalminds.beej.core.VoiceJudge
import com.digitalminds.beej.core.VoiceJudgement
import com.digitalminds.beej.core.ContestFailure
import com.digitalminds.beej.core.Words
import com.digitalminds.beej.voice.VoiceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CPhase { INTRO, SHOWING, LISTENING, RESULT }

/**
 * Simulador del concurso real: se muestra la palabra unos 2,3 s mientras suena, desaparece,
 * y el alumno responde en voz alta: PALABRA, deletreo, PALABRA. Un error termina la ronda.
 */
@Composable
fun ContestScreen(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    val voiceState by vm.voice.state.collectAsState()
    val goal = remember { vm.dayPlan().simulationRounds }

    var phase by remember { mutableStateOf(CPhase.INTRO) }
    var order by remember { mutableStateOf(Words.all.map { it.text }.shuffled()) }
    var idx by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(vm.bestContest) }
    var result by remember { mutableStateOf<VoiceJudgement?>(null) }
    var ready by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }
    val stop = remember { BooleanArray(1) }
    val repeat = remember { BooleanArray(1) }

    fun playRound() {
        scope.launch {
            val word = order[idx]
            var retries = 0
            result = null; notice = ""; ready = false
            phase = CPhase.SHOWING
            vm.playWord(word)
            delay(2300)
            var final: VoiceJudgement? = null
            while (final == null) {
                phase = CPhase.LISTENING
                ready = false; stop[0] = false; repeat[0] = false
                val cap = vm.voice.listen(word, true, { ready = true }, { stop[0] })
                if (repeat[0]) {
                    // "Could you repeat the word, please?"
                    repeat[0] = false
                    notice = ""
                    vm.playWord(word)
                    delay(1500)
                    continue
                }
                val j = VoiceJudge.judge(word, cap.forced, cap.free, true)
                if ((j.outcome == JudgeOutcome.NOTHING || j.outcome == JudgeOutcome.UNSURE) && retries < 2) {
                    retries++
                    notice = if (j.outcome == JudgeOutcome.NOTHING) "No te escuché. Repite." else "No te entendí bien. Repite."
                    delay(600)
                    continue
                }
                final = j
            }
            val fr = final ?: return@launch
            result = fr
            val ok = fr.outcome == JudgeOutcome.CORRECT
            vm.record(word, ok)
            if (ok) {
                score++
                if (score > best) { best = score; vm.bestContest = score }
                if (idx + 1 >= order.size) finished = true
            }
            phase = CPhase.RESULT
        }
    }

    val micGate = rememberMicGate { playRound() }

    fun restart() {
        order = Words.all.map { it.text }.shuffled()
        idx = 0; score = 0; finished = false; result = null; notice = ""
        phase = CPhase.INTRO
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        BackBar(onBack = { vm.pop() }) {
            if (phase != CPhase.INTRO) {
                Text("Correctas: $score", style = Type.Label.copy(color = Brand.TextPrimary))
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            when (phase) {
                CPhase.INTRO -> Intro(voiceState, best, goal)
                CPhase.SHOWING -> Text(
                    order[idx],
                    style = Type.WordBig.copy(fontSize = if (order[idx].length > 10) 32.sp else 42.sp),
                    textAlign = TextAlign.Center, maxLines = 1
                )
                CPhase.LISTENING -> Listening(ready, notice)
                CPhase.RESULT -> ResultView(result!!, score, best, finished)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (phase) {
                CPhase.INTRO -> SquareButton(
                    R.drawable.ic_mic, "Empezar", { micGate() },
                    accent = true, enabled = voiceState is VoiceState.Ready, size = 64.dp
                )
                CPhase.SHOWING -> {}
                CPhase.LISTENING -> {
                    SquareButton(R.drawable.ic_review, "Repetir la palabra", { repeat[0] = true; stop[0] = true })
                    Spacer(Modifier.width(24.dp))
                    SquareButton(R.drawable.ic_check, "Listo", { stop[0] = true }, accent = true, size = 64.dp)
                }
                CPhase.RESULT -> {
                    val ok = result?.outcome == JudgeOutcome.CORRECT && !finished
                    if (ok) {
                        SquareButton(R.drawable.ic_next, "Siguiente palabra", { idx++; playRound() }, accent = true, size = 64.dp)
                    } else {
                        SquareButton(R.drawable.ic_review, "Otra vez", { restart() }, accent = true, size = 64.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Intro(voiceState: VoiceState, best: Int, goal: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(painterResource(R.drawable.ic_contest), null, tint = Brand.RedBright, modifier = Modifier.size(44.dp))
        Text("PALABRA · DELETREO · PALABRA", style = Type.Label.copy(letterSpacing = 1.sp))
        when (voiceState) {
            is VoiceState.Failed -> Text(voiceState.reason + " Usa Entrenar en modo escrito.", style = Type.BodySecondary, textAlign = TextAlign.Center)
            is VoiceState.Ready -> {
                val extra = if (goal > 0) "  ·  Meta de hoy: $goal seguidas" else ""
                Text("Mejor: $best$extra", style = Type.BodySecondary, textAlign = TextAlign.Center)
            }
            else -> Text("Preparando la voz…", style = Type.BodySecondary)
        }
    }
}

@Composable
private fun Listening(ready: Boolean, notice: String) {
    val t = rememberInfiniteTransition(label = "pulse")
    val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            Modifier.size(92.dp).alpha(if (ready) a else 0.25f).clip(CircleShape).background(Brand.Ferrari),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_mic), null, tint = Brand.TextPrimary, modifier = Modifier.size(38.dp))
        }
        if (notice.isNotBlank()) Text(notice, style = Type.Body.copy(color = Brand.Error), textAlign = TextAlign.Center)
        Text(if (ready) "Palabra, deletreo, palabra" else "Preparando…", style = Type.BodySecondary, textAlign = TextAlign.Center)
        Text("Could you repeat the word, please?", style = Type.Label.copy(fontSize = 11.sp))
    }
}

@Composable
private fun ResultView(r: VoiceJudgement, score: Int, best: Int, finished: Boolean) {
    val correct = r.outcome == JudgeOutcome.CORRECT
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (correct) {
            Icon(painterResource(R.drawable.ic_check), null, tint = Brand.TextPrimary, modifier = Modifier.size(40.dp))
            Text(if (finished) "¡Las 30 palabras!" else "Correcto", style = Type.Card.copy(fontSize = 22.sp))
            FluencyLine(r)
        } else {
            Text("Incorrecto", style = Type.Card.copy(fontSize = 22.sp, color = Brand.Error))
            Text(r.word, style = Type.Row)
            when {
                r.outcome == JudgeOutcome.NOTHING -> Text("No se escuchó nada.", style = Type.BodySecondary)
                r.outcome == JudgeOutcome.UNSURE ->
                    Text("No pude confirmar tu deletreo. Habla claro y sin pausas largas.", style = Type.BodySecondary, textAlign = TextAlign.Center)
                r.failure == ContestFailure.MISSING_START_WORD -> Text("Faltó decir la palabra al inicio.", style = Type.BodySecondary)
                r.failure == ContestFailure.MISSING_END_WORD -> Text("Faltó repetir la palabra al final.", style = Type.BodySecondary)
                r.failure == ContestFailure.WRONG_SPELLING -> {
                    DiffLines(r.spelling)
                    if (!r.startWordOk || !r.endWordOk) Text("Recuerda: palabra, deletreo, palabra.", style = Type.BodySecondary)
                }
            }
            FluencyLine(r)
            Text("Rondas correctas: $score  ·  Mejor: $best", style = Type.Label)
        }
    }
}
