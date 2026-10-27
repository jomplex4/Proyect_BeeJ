package com.digitalminds.beej.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalminds.beej.AppViewModel
import com.digitalminds.beej.R
import com.digitalminds.beej.Screen
import com.digitalminds.beej.core.Phase
import com.digitalminds.beej.core.ProgressLogic
import com.digitalminds.beej.core.StudyPlan

// --------------------------------------------------------------------------------------------
// Repasar errores
// --------------------------------------------------------------------------------------------
@Composable
fun ReviewScreen(vm: AppViewModel) {
    val progress by vm.progress.collectAsState()
    val errors = ProgressLogic.errorWords(progress)
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        BackBar(onBack = { vm.pop() })
        if (errors.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(painterResource(R.drawable.ic_check), null, tint = Brand.TextSecondary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("Sin errores pendientes", style = Type.BodySecondary)
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (w in errors) {
                    val s = ProgressLogic.statsOf(progress, w)
                    Row(
                        Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Card).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Dot(Brand.Error, 9.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(w, style = Type.Row, modifier = Modifier.weight(1f))
                        Text("${s.correctSinceError}/${ProgressLogic.CORRECT_TO_CLEAR}", style = Type.Label)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brand.Ferrari)
                        .clickable { vm.push(Screen.Train(errors.shuffled(), 0, review = true)) }
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_review), null, tint = Brand.TextPrimary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Repasar ahora", style = Type.Card)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------------
// Alfabeto
// --------------------------------------------------------------------------------------------
private val IPA = mapOf(
    'A' to "eɪ", 'B' to "biː", 'C' to "siː", 'D' to "diː", 'E' to "iː", 'F' to "ɛf", 'G' to "dʒiː",
    'H' to "eɪtʃ", 'I' to "aɪ", 'J' to "dʒeɪ", 'K' to "keɪ", 'L' to "ɛl", 'M' to "ɛm", 'N' to "ɛn",
    'O' to "oʊ", 'P' to "piː", 'Q' to "kjuː", 'R' to "ɑr", 'S' to "ɛs", 'T' to "tiː", 'U' to "juː",
    'V' to "viː", 'W' to "ˈdʌbəl juː", 'X' to "ɛks", 'Y' to "waɪ", 'Z' to "ziː"
)

/** Pares que las reglas del concurso piden pronunciar con énfasis, más dos que el oído confunde. */
private val PAIRS = listOf('B' to 'V', 'M' to 'N', 'C' to 'Z', 'T' to 'D', 'A' to 'I', 'G' to 'J')

@Composable
fun AlphabetScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        BackBar(onBack = { vm.pop() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ('A'..'Z').toList().chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in row) {
                        Column(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brand.Card)
                                .clickable { vm.playLetter(c) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(c.toString(), style = Type.Letter.copy(fontSize = 30.sp))
                            Text(IPA[c] ?: "", style = Type.Label.copy(fontSize = 11.sp))
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Pares que se confunden", style = Type.Label)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((a, b) in PAIRS.take(3)) PairChip(a, b, Modifier.weight(1f)) { vm.playPair(a, b) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((a, b) in PAIRS.drop(3)) PairChip(a, b, Modifier.weight(1f)) { vm.playPair(a, b) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PairChip(a: Char, b: Char, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Panel).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text("$a  ·  $b", style = Type.Letter.copy(fontSize = 20.sp)) }
}

// --------------------------------------------------------------------------------------------
// Método
// --------------------------------------------------------------------------------------------
private val STEPS = listOf(
    "Escucha la palabra.",
    "Repítela en voz alta.",
    "Escucha el deletreo, letra por letra.",
    "Deletrea sin mirar.",
    "Escríbela de memoria.",
    "Comprueba y corrige al instante.",
    "Repite las falladas más tarde: hoy y mañana.",
    "Simula el concurso: palabra, deletreo, palabra.",
    "Al hablar, no pauses más de 1 segundo entre letras."
)

@Composable
fun MethodScreen(vm: AppViewModel) {
    val day = vm.planDay
    val plan = vm.dayPlan()
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        BackBar(onBack = { vm.pop() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            STEPS.forEachIndexed { i, text ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Brand.Ferrari), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = Type.Label.copy(color = Brand.TextPrimary))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(text, style = Type.Body)
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Panel).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("CAREFUL", style = Type.Row)
                Text("C, A, R, E, F, U, L", style = Type.Letter.copy(fontSize = 18.sp, color = Brand.TextSecondary))
                Text("CAREFUL", style = Type.Row)
            }
            Spacer(Modifier.height(4.dp))
            Text("Plan de 14 días", style = Type.Label)
            PhaseRow("Días 1 a 5", "6 palabras nuevas por día y repaso del día anterior.", day in 1..5)
            PhaseRow("Días 6 a 10", "Consolidar: bloque, palabras largas y errores, con 3 rondas de concurso.", day in 6..10)
            PhaseRow("Días 11 a 14", "Todo mezclado, sin pistas, con simulaciones de 5 a 10 rondas.", day >= 11)
            if (plan.focus.isNotEmpty()) {
                Text("Hoy, día $day: ${plan.focus.size} palabras con punto rojo en las listas.", style = Type.BodySecondary)
            }
            Row(
                Modifier
                    .padding(top = 6.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brand.Panel)
                    .clickable { vm.restartPlan() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { Text("Empezar el plan en el día 1", style = Type.Label) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PhaseRow(title: String, text: String, current: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card).padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.padding(top = 6.dp)) { Dot(if (current) Brand.RedBright else Brand.Panel) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = Type.Card.copy(fontSize = 15.sp))
            Text(text, style = Type.BodySecondary)
        }
    }
}
