package com.digitalminds.beej

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalminds.beej.audio.AudioEngine
import com.digitalminds.beej.core.DayPlan
import com.digitalminds.beej.core.Progress
import com.digitalminds.beej.core.ProgressLogic
import com.digitalminds.beej.core.Speed
import com.digitalminds.beej.core.SpellPlanner
import com.digitalminds.beej.core.StudyPlan
import com.digitalminds.beej.core.Words
import com.digitalminds.beej.data.Store
import com.digitalminds.beej.voice.VoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface Screen {
    data object Home : Screen
    data object LearnList : Screen
    data class LearnWord(val index: Int) : Screen
    data object TrainList : Screen
    /** Sesión de entrenamiento con una cola de palabras. [review] = repaso de errores. */
    data class Train(val queue: List<String>, val index: Int, val review: Boolean = false) : Screen
    data object Contest : Screen
    data object Review : Screen
    data object Alphabet : Screen
    data object Method : Screen
    data object About : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val audio = AudioEngine(app)
    val voice = VoiceEngine(app)

    private val _progress = MutableStateFlow(store.loadProgress())
    val progress: StateFlow<Progress> = _progress

    private val _wordSpeed = MutableStateFlow(store.wordSpeed)
    val wordSpeed: StateFlow<Speed> = _wordSpeed
    private val _spellSpeed = MutableStateFlow(store.spellSpeed)
    val spellSpeed: StateFlow<Speed> = _spellSpeed
    private val _doubleMode = MutableStateFlow(store.doubleMode)
    val doubleMode: StateFlow<Boolean> = _doubleMode

    /** Letras que se están resaltando mientras suena el deletreo (null = ninguna). */
    private val _highlight = MutableStateFlow<IntRange?>(null)
    val highlight: StateFlow<IntRange?> = _highlight

    val stack = mutableStateListOf<Screen>(Screen.Home)

    /** Modo de entrenamiento elegido: false = escrito, true = oral. */
    var trainOral by mutableStateOf(false)
    private var audioJob: Job? = null

    init {
        val today = LocalDate.now().toEpochDay()
        if (_progress.value.startEpochDay < 0) update(_progress.value.copy(startEpochDay = today))
        viewModelScope.launch(Dispatchers.IO) {
            audio.preload(AudioEngine.filesForWordSpeed(_wordSpeed.value, Words.all.map { it.text }))
            audio.preload(AudioEngine.filesForLetterSpeed(_spellSpeed.value))
            audio.preload(AudioEngine.filesForLetterSpeed(Speed.NORMAL))
            delay(1200)
            voice.prepare()
        }
    }

    private fun update(p: Progress) {
        _progress.value = p
        store.saveProgress(p)
    }

    val today: Long get() = LocalDate.now().toEpochDay()

    val planDay: Int
        get() = StudyPlan.dayFor(_progress.value.startEpochDay.takeIf { it >= 0 } ?: today, today)

    fun dayPlan(): DayPlan = StudyPlan.forDay(planDay, ProgressLogic.errorWords(_progress.value))

    fun restartPlan() = update(_progress.value.copy(startEpochDay = today))

    var bestContest: Int
        get() = store.bestContest
        set(v) { store.bestContest = v }

    // ---- navegación
    fun push(s: Screen) { stopAudio(); stack.add(s) }
    fun replaceTop(s: Screen) { stopAudio(); stack[stack.lastIndex] = s }
    fun pop() { stopAudio(); if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    // ---- ajustes
    fun setWordSpeed(s: Speed) {
        _wordSpeed.value = s; store.wordSpeed = s
        viewModelScope.launch(Dispatchers.IO) { audio.preload(AudioEngine.filesForWordSpeed(s, Words.all.map { it.text })) }
    }
    fun setSpellSpeed(s: Speed) {
        _spellSpeed.value = s; store.spellSpeed = s
        viewModelScope.launch(Dispatchers.IO) { audio.preload(AudioEngine.filesForLetterSpeed(s)) }
    }
    fun setDoubleMode(v: Boolean) { _doubleMode.value = v; store.doubleMode = v }

    // ---- audio
    fun stopAudio() {
        audioJob?.cancel(); audioJob = null
        audio.stopAll()
        _highlight.value = null
    }

    fun playWord(word: String) {
        stopAudio()
        audioJob = viewModelScope.launch { audio.playWord(word, _wordSpeed.value) }
    }

    fun playSpelling(word: String) {
        stopAudio()
        val speed = _spellSpeed.value
        val dbl = _doubleMode.value
        val steps = SpellPlanner.spellSteps(word, dbl, speed)
        val spans = SpellPlanner.letterSpans(word, dbl)
        audioJob = viewModelScope.launch {
            audio.playSteps(steps, speed) { i -> _highlight.value = spans.getOrNull(i) }
        }
    }

    fun playLetter(letter: Char) {
        stopAudio()
        audioJob = viewModelScope.launch { audio.play(SpellPlanner.fileFor(SpellPlanner.letterClip(letter), Speed.NORMAL)) }
    }

    /** Dos letras seguidas, para practicar pares que se confunden. */
    fun playPair(a: Char, b: Char) {
        stopAudio()
        audioJob = viewModelScope.launch {
            audio.play(SpellPlanner.fileFor(SpellPlanner.letterClip(a), Speed.NORMAL))
            delay(550)
            audio.play(SpellPlanner.fileFor(SpellPlanner.letterClip(b), Speed.NORMAL))
        }
    }

    // ---- progreso
    fun record(word: String, correct: Boolean) {
        update(ProgressLogic.record(_progress.value, word, correct, today))
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }
}
