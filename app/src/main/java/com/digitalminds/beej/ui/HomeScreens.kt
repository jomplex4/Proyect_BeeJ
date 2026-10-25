package com.digitalminds.beej.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalminds.beej.AppViewModel
import com.digitalminds.beej.R
import com.digitalminds.beej.Screen
import com.digitalminds.beej.core.ProgressLogic
import com.digitalminds.beej.core.StudyPlan

/** Misma composición que el splash del sistema, para que la transición no se note. */
@Composable
fun SplashContent() {
    Box(Modifier.fillMaxSize().background(Brand.Black)) {
        Image(
            painterResource(R.drawable.splash_icon), null,
            modifier = Modifier.size(288.dp).align(Alignment.Center)
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("POWERED BY", style = Type.Wordmark)
            Spacer(Modifier.height(12.dp))
            Image(
                painterResource(R.drawable.splash_brand), "DIGITALMINDS",
                modifier = Modifier.height(20.dp).aspectRatio(720f / 111f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val progress by vm.progress.collectAsState()
    val errors = ProgressLogic.errorWords(progress).size
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.splash_brand), "DIGITALMINDS",
                modifier = Modifier.height(22.dp).aspectRatio(720f / 111f),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brand.Panel)
                    .clickable { vm.push(Screen.Method) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Dot(Brand.RedBright, 7.dp)
                Spacer(Modifier.width(8.dp))
                Text("Día ${vm.planDay}/${StudyPlan.DAYS}", style = Type.Label.copy(color = Brand.TextPrimary))
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Brand.Panel).clickable { vm.push(Screen.About) },
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_info), "Acerca de", tint = Brand.TextPrimary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ModuleCard(R.drawable.ic_learn, "Aprender", null) { vm.push(Screen.LearnList) }
            ModuleCard(R.drawable.ic_train, "Entrenar", null) { vm.push(Screen.TrainList) }
            ModuleCard(R.drawable.ic_contest, "Concurso", null) { vm.push(Screen.Contest) }
            ModuleCard(R.drawable.ic_review, "Repasar errores", if (errors > 0) errors.toString() else null) { vm.push(Screen.Review) }
            ModuleCard(R.drawable.ic_alphabet, "Alfabeto", null) { vm.push(Screen.Alphabet) }
            ModuleCard(R.drawable.ic_method, "Método", null) { vm.push(Screen.Method) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModuleCard(icon: Int, title: String, badge: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brand.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Panel),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(icon), null, tint = Brand.RedBright, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, style = Type.Card, modifier = Modifier.weight(1f))
        if (badge != null) {
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Brand.Ferrari).padding(horizontal = 9.dp, vertical = 3.dp)
            ) { Text(badge, style = Type.Label.copy(color = Brand.TextPrimary)) }
            Spacer(Modifier.width(10.dp))
        }
        Icon(painterResource(R.drawable.ic_next), null, tint = Brand.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun AboutScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val version = remember0 {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        BackBar(onBack = { vm.pop() })
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painterResource(R.drawable.ic_mark), "BEE J", modifier = Modifier.size(150.dp))
            Spacer(Modifier.height(20.dp))
            Text("BEE J", style = Type.Card.copy(fontFamily = Michroma, fontSize = 22.sp, letterSpacing = 4.sp))
            Spacer(Modifier.height(6.dp))
            Text("Versión $version", style = Type.BodySecondary)
            Spacer(Modifier.height(18.dp))
            Text("Creado por Joel Mamani Pauccara", style = Type.Body, textAlign = TextAlign.Center)
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("POWERED BY", style = Type.Wordmark)
            Spacer(Modifier.height(12.dp))
            Image(
                painterResource(R.drawable.splash_brand), "DIGITALMINDS",
                modifier = Modifier.height(20.dp).aspectRatio(720f / 111f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun <T> remember0(calc: () -> T): T = androidx.compose.runtime.remember { calc() }
