package com.digitalminds.beej.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.digitalminds.beej.R
import com.digitalminds.beej.core.Op
import com.digitalminds.beej.core.Speed
import com.digitalminds.beej.core.SpellingResult
import com.digitalminds.beej.core.Verdict

/** Botón compacto cuadrado con ícono. Área táctil cómoda (mínimo 48 dp). */
@Composable
fun SquareButton(
    icon: Int,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (accent) Brand.Ferrari else Brand.Panel)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), contentDescription = desc, tint = Brand.TextPrimary, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Brand.Ferrari else Brand.Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = Type.Label.copy(color = if (selected) Brand.TextPrimary else Brand.TextSecondary))
    }
}

/** Barra superior mínima: solo flecha atrás y, si hace falta, acciones a la derecha. */
@Composable
fun BackBar(onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareButton(R.drawable.ic_back, "Atrás", onBack, size = 48.dp)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun SpeedPicker(icon: Int, desc: String, current: Speed, onChange: (Speed) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = desc, tint = Brand.TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (s in Speed.values()) Chip(s.label, current == s, { onChange(s) })
        }
    }
}

/** Devuelve una función que pide el permiso del micrófono si falta y luego ejecuta [onGranted]. */
@Composable
fun rememberMicGate(onGranted: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val current by rememberUpdatedState(onGranted)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) current()
    }
    return remember {
        {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                current()
            } else {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

fun verdictLabel(v: Verdict): String = when (v) {
    Verdict.PERFECT -> "Perfecto"
    Verdict.ALMOST -> "Casi bien"
    Verdict.REINFORCE -> "Debes reforzar"
}

/** Esperado y entendido, letra por letra, marcando dónde estuvo el error. */
@Composable
fun DiffLines(result: SpellingResult, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(74.dp)) {
            Text("Esperado", style = Type.Label.copy(fontSize = 12.sp), modifier = Modifier.height(32.dp))
            Text("Entendí", style = Type.Label.copy(fontSize = 12.sp), modifier = Modifier.height(32.dp))
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            for (s in result.steps) {
                val ok = s.op == Op.MATCH
                Column(Modifier.width(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        (s.expected ?: '·').toString(),
                        style = Type.Letter.copy(fontSize = 18.sp, color = Brand.TextPrimary),
                        modifier = Modifier.height(32.dp), textAlign = TextAlign.Center
                    )
                    Text(
                        (s.heard ?: '-').toString(),
                        style = Type.Letter.copy(fontSize = 18.sp, color = if (ok) Brand.TextSecondary else Brand.Error),
                        modifier = Modifier.height(32.dp), textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Teclado propio solo de letras: no hay forma de escribir otra cosa. */
@Composable
fun LetterKeyboard(onKey: (Char) -> Unit, onBackspace: () -> Unit, enabled: Boolean = true) {
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .alpha(if (enabled) 1f else 0.4f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEachIndexed { r, keys ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (r == 1) Spacer(Modifier.weight(0.5f))
                if (r == 2) Spacer(Modifier.weight(0.5f))
                for (k in keys) Key(k.toString(), Modifier.weight(1f)) { if (enabled) onKey(k) }
                if (r == 1) Spacer(Modifier.weight(0.5f))
                if (r == 2) {
                    Box(
                        Modifier
                            .weight(2f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brand.Panel)
                            .clickable(enabled = enabled, onClick = onBackspace),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_backspace), "Borrar", tint = Brand.TextPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.weight(0.5f))
                }
            }
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brand.Card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = Type.Letter.copy(fontSize = 19.sp))
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(50)).background(color))
}
