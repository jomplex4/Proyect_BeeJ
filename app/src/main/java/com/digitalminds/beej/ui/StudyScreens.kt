package com.digitalminds.beej.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalminds.beej.AppViewModel
import com.digitalminds.beej.R
import com.digitalminds.beej.Screen
import com.digitalminds.beej.core.ContestEvaluator
import com.digitalminds.beej.core.SpellPlanner
import com.digitalminds.beej.core.SpellingEvaluator
import com.digitalminds.beej.core.SpellingResult
import com.digitalminds.beej.core.ProgressLogic
import com.digitalminds.beej.core.Verdict
import com.digitalminds.beej.core.Words
import com.digitalminds.beej.voice.VoiceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Lista directa de las 30 palabras. Las de hoy (según el plan) van primero, marcadas con un punto rojo. */
@Composable
fun WordListScreen(vm: AppViewModel, onPick: (Int) -> Unit) {
    val progress by vm.progress.collectAsState()
    val focus = vm.dayPlan().focus
    val all = Words.all.map { it.text }
    val ordered = focus.filter { it in all } + all.filter { it !in focus }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        BackBar(onBack = { vm.pop() })
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ordered) { w ->
                val today = w in focus
                val stats = ProgressLogic.statsOf(progress, w)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brand.Card)
                        .clickable { onPick(all.indexOf(w)) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (today) { Dot(Brand.RedBright); Spacer(Modifier.width(12.dp)) }
                    Text(w, style = Type.Row, modifier = Modifier.weight(1f))
                    when {
                        stats.inErrorBook -> Dot(Brand.Error, 9.dp)
                        ProgressLogic.isMastered(progress, w) ->
                            Icon(painterResource(R.drawable.ic_check), null, tint = Brand.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun LearnWordScreen(vm: AppViewModel, index: Int) {
    val w = Words.all[index]
    val wordSpeed by vm.wordSpeed.collectAsState()
    val spellSpeed by vm.spellSpeed.collectAsState()
    val dbl by vm.doubleMode.collectAsState()
    val highlight by vm.highlight.collectAsState()

    LaunchedEffect(index) {
        delay(250)
        vm.playWord(w.text)
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        BackBar(onBack = { vm.pop() })
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val size = when {
                w.text.length <= 8 -> 44
                w.text.length <= 10 -> 38
                w.text.length <= 12 -> 33
                else -> 29
            }
            Text(w.text, style = Type.WordBig.copy(fontSize = size.sp, letterSpacing = 2.sp), textAlign = TextAlign.Center, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(w.es, style = Type.BodySecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            Text(letterStrip(w.text, highlight), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(SpellPlanner.spellText(w.text, dbl), style = Type.Label.copy(fontSize = 12.sp), textAlign = TextAlign.Center)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brand.Card)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SpeedPicker(R.drawable.ic_speaker, "Velocidad de la palabra", wordSpeed) { vm.setWordSpeed(it) }
            SpeedPicker(R.drawable.ic_spell, "Velocidad del deletreo", spellSpeed) { vm.setSpellSpeed(it) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Repetidas", style = Type.Label, modifier = Modifier.width(34.dp + 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("F, F", !dbl, { vm.setDoubleMode(false) })
                    Chip("DOUBLE F", dbl, { vm.setDoubleMode(true) })
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SquareButton(R.drawable.ic_back, "Anterior", { vm.replaceTop(Screen.LearnWord((index + Words.all.size - 1) % Words.all.size)) })
            SquareButton(R.drawable.ic_speaker, "Escuchar palabra", { vm.playWord(w.text) }, accent = true)
            SquareButton(R.drawable.ic_spell, "Escuchar deletreo", { vm.playSpelling(w.text) }, accent = true)
            SquareButton(R.drawable.ic_next, "Siguiente", { vm.replaceTop(Screen.LearnWord((index + 1) % Words.all.size)) })
        }
    }
}

private fun letterStrip(word: String, highlight: IntRange?): AnnotatedString = buildAnnotatedString {
    word.forEachIndexed { i, c ->
        val on = highlight != null && i in highlight
        withStyle(
            SpanStyle(
                fontFamily = Atkinson,
                fontSize = 22.sp,
                color = if (on) Brand.RedBright else Brand.TextPrimary
            )
        ) { append(c.toString()) }
        if (i < word.lastIndex) withStyle(SpanStyle(fontFamily = Atkinson, fontSize = 22.sp, color = Brand.TextSecondary)) { append("-") }
    }
}

// --------------------------------------------------------------------------------------------
// Entrenamiento (oral y escrito)
// --------------------------------------------------------------------------------------------

@Composable
fun TrainScreen(vm: AppViewModel, s: Screen.Train) {
    val word = s.queue[s.index]
    val oral = vm.trainOral
    val scope = rememberCoroutineScope()
    val voiceState by vm.voice.state.collectAsState()

    var typed by remember(word) { mutableStateOf("") }
    var result by remember(word) { mutableStateOf<SpellingResult?>(null) }
    var uncertain by remember(word) { mutableStateOf(false) }
    var listening by remember(word) { mutableStateOf(false) }
    var partial by remember(word) { mutableStateOf("") }
    val stop = remember(word) { BooleanArray(1) }

    LaunchedEffect(word) {
        delay(300)
        vm.playWord(word)
    }

    fun go(delta: Int) {
        val n = s.queue.size
        vm.replaceTop(s.copy(index = (s.index + delta + n) % n))
    }

    fun startListening() {
        if (listening) return
        listening = true; partial = ""; uncertain = false; result = null; stop[0] = false
        scope.launch {
            val toks = vm.voice.listen({ partial = it }, { stop[0] })
            listening = false
            val texts = SpellingEvaluator.stripWordEnds(toks.map { it.text }, word)
            val r = SpellingEvaluator.evaluateTokens(word, texts)
            val lowConf = toks.any { it.conf < ContestEvaluator.LOW_CONFIDENCE }
            if (toks.isEmpty()) {
                partial = ""
                uncertain = true
            } else if (!r.exact && lowConf) {
                uncertain = true
                result = null
            } else {
                result = r
                vm.record(word, r.exact)
            }
        }
    }
    val micGate = rememberMicGate { startListening() }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        BackBar(onBack = { vm.pop() }) {
            SquareButton(R.drawable.ic_pencil, "Escrito", { vm.trainOral = false }, accent = !oral, size = 48.dp)
            Spacer(Modifier.width(8.dp))
            SquareButton(R.drawable.ic_mic, "Oral", { vm.trainOral = true }, accent = oral, size = 48.dp)
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!oral) {
                SpelledCells(typed, result)
                Spacer(Modifier.height(20.dp))
                result?.let { ResultBlock(it) }
            } else {
                val cur = result
                when {
                    voiceState is VoiceState.Failed ->
                        Text((voiceState as VoiceState.Failed).reason + " Usa el modo escrito.", style = Type.BodySecondary, textAlign = TextAlign.Center)
                    voiceState !is VoiceState.Ready ->
                        Text("Preparando la voz…", style = Type.BodySecondary)
                    listening ->
                        Text(if (partial.isBlank()) "Te escucho…" else partial, style = Type.Body.copy(fontSize = 18.sp), textAlign = TextAlign.Center)
                    uncertain ->
                        Text("No te escuché bien. Inténtalo otra vez.", style = Type.BodySecondary, textAlign = TextAlign.Center)
                    cur != null -> ResultBlock(cur)
                    else -> Text("Di las letras de la palabra", style = Type.BodySecondary)
                }
            }
        }

        if (!oral) {
            LetterKeyboard(
                onKey = { c -> if (result == null && typed.length < 20) typed += c },
                onBackspace = { if (result == null && typed.isNotEmpty()) typed = typed.dropLast(1) }
            )
            Spacer(Modifier.height(14.dp))
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SquareButton(R.drawable.ic_back, "Anterior", { go(-1) })
            SquareButton(R.drawable.ic_speaker, "Escuchar palabra", { vm.playWord(word) })
            if (!oral) {
                if (result == null) {
                    SquareButton(R.drawable.ic_check, "Comprobar", {
                        if (typed.isNotEmpty()) {
                            val r = SpellingEvaluator.evaluate(word, typed)
                            result = r
                            vm.record(word, r.exact)
                        }
                    }, accent = true, enabled = typed.isNotEmpty())
                } else {
                    SquareButton(R.drawable.ic_review, "Reintentar", { typed = ""; result = null }, accent = true)
                }
            } else {
                SquareButton(
                    if (listening) R.drawable.ic_check else R.drawable.ic_mic,
                    if (listening) "Listo" else "Hablar",
                    { if (listening) { stop[0] = true } else { micGate() } },
                    accent = true,
                    enabled = voiceState is VoiceState.Ready
                )
            }
            SquareButton(R.drawable.ic_next, "Siguiente", { go(1) })
        }
    }
}

/** Letras tecleadas con aspecto de deletreo: C - A - R _ . No revela cuántas letras tiene la palabra. */
@Composable
private fun SpelledCells(typed: String, result: SpellingResult?) {
    val shown = if (result != null) typed else typed + "_"
    val rows = shown.chunked(7)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEachIndexed { ri, row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                row.forEachIndexed { i, c ->
                    val isCursor = result == null && c == '_' && ri == rows.lastIndex && i == row.lastIndex
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCursor) Brand.Black else Brand.Panel),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            c.toString(),
                            style = Type.Letter.copy(color = if (isCursor) Brand.RedBright else Brand.TextPrimary)
                        )
                    }
                    if (i < row.lastIndex) Text("-", style = Type.Label, modifier = Modifier.width(10.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ResultBlock(r: SpellingResult) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (r.verdict == Verdict.PERFECT) {
                Icon(painterResource(R.drawable.ic_check), null, tint = Brand.TextPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                verdictLabel(r.verdict),
                style = Type.Card.copy(color = if (r.verdict == Verdict.PERFECT) Brand.TextPrimary else Brand.Error)
            )
        }
        if (!r.exact) DiffLines(r)
    }
}
