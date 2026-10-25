package com.digitalminds.beej

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.digitalminds.beej.ui.AboutScreen
import com.digitalminds.beej.ui.AlphabetScreen
import com.digitalminds.beej.ui.BeeJTheme
import com.digitalminds.beej.ui.Brand
import com.digitalminds.beej.ui.ContestScreen
import com.digitalminds.beej.ui.HomeScreen
import com.digitalminds.beej.ui.LearnWordScreen
import com.digitalminds.beej.ui.MethodScreen
import com.digitalminds.beej.ui.ReviewScreen
import com.digitalminds.beej.ui.SplashContent
import com.digitalminds.beej.ui.TrainScreen
import com.digitalminds.beej.ui.WordListScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            BeeJTheme {
                BeeJApp(vm)
            }
        }
    }
}

@Composable
fun BeeJApp(vm: AppViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1100)
        showSplash = false
    }
    Box(Modifier.fillMaxSize().background(Brand.Black)) {
        if (showSplash) {
            SplashContent()
        } else {
            val screen = vm.stack.last()
            BackHandler(enabled = vm.stack.size > 1) { vm.pop() }
            when (screen) {
                Screen.Home -> HomeScreen(vm)
                Screen.LearnList -> WordListScreen(vm) { i -> vm.push(Screen.LearnWord(i)) }
                is Screen.LearnWord -> LearnWordScreen(vm, screen.index)
                Screen.TrainList -> WordListScreen(vm) { i ->
                    val all = com.digitalminds.beej.core.Words.all.map { it.text }
                    vm.push(Screen.Train(all, i))
                }
                is Screen.Train -> TrainScreen(vm, screen)
                Screen.Contest -> ContestScreen(vm)
                Screen.Review -> ReviewScreen(vm)
                Screen.Alphabet -> AlphabetScreen(vm)
                Screen.Method -> MethodScreen(vm)
                Screen.About -> AboutScreen(vm)
            }
        }
    }
}
