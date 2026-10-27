package com.digitalminds.beej.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.digitalminds.beej.core.PhraseGrammar
import com.digitalminds.beej.core.TimedToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Preparing : VoiceState
    data object Ready : VoiceState
    data class Failed(val reason: String) : VoiceState
}

/**
 * Reconocimiento de voz 100 % local con Vosk (modelo pequeño en inglés incluido en el APK).
 * Usa una gramática cerrada: solo las 30 palabras, los nombres de las letras y "double".
 */
class VoiceEngine(private val context: Context) {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: kotlinx.coroutines.flow.StateFlow<VoiceState> = _state

    @Volatile private var model: Model? = null

    /** Segunda lectura: cualquier letra, "double" y las 30 palabras. */
    private val freeGrammar: String by lazy { PhraseGrammar.toJson(PhraseGrammar.free()) }

    /** Las dos lecturas del mismo audio. */
    class Capture(val forced: List<TimedToken>, val free: List<TimedToken>)

    suspend fun prepare() = withContext(Dispatchers.IO) {
        if (model != null) return@withContext
        _state.value = VoiceState.Preparing
        try {
            val assetDir = "model-en-us"
            val children = context.assets.list(assetDir)
            if (children == null || children.isEmpty()) {
                _state.value = VoiceState.Failed("Este APK no incluye el modelo de voz.")
                return@withContext
            }
            val dest = File(context.filesDir, "vosk-model")
            val marker = File(dest, ".ready_$MODEL_VERSION")
            if (!marker.exists()) {
                dest.deleteRecursively()
                dest.mkdirs()
                copyAssets(assetDir, dest)
                marker.writeText("ok")
            }
            LibVosk.setLogLevel(LogLevel.WARNINGS)
            model = Model(dest.absolutePath)
            _state.value = VoiceState.Ready
        } catch (e: Throwable) {
            _state.value = VoiceState.Failed("No se pudo preparar la voz en este teléfono.")
        }
    }

    private fun copyAssets(path: String, dest: File) {
        val list: Array<String> = context.assets.list(path) ?: emptyArray<String>()
        if (list.isEmpty()) {
            try {
                context.assets.open(path).use { input ->
                    dest.parentFile?.mkdirs()
                    FileOutputStream(dest).use { out -> input.copyTo(out) }
                }
            } catch (e: Exception) {
                dest.mkdirs() // directorio vacío
            }
        } else {
            dest.mkdirs()
            for (name in list) copyAssets("$path/$name", File(dest, name))
        }
    }

    /**
     * Escucha hasta que el alumno termina de hablar (silencio), se toca "listo" o se agota el tiempo.
     * Lee el audio DOS veces a la vez: con la gramática de esa palabra y con una gramática libre.
     * @param word palabra que se debe deletrear.
     * @param contest true: formato PALABRA, deletreo, PALABRA.
     * @param onReady se avisa cuando el micrófono ya está grabando (preparar la gramática tarda un instante).
     * @param stopRequested devuelve true si el usuario pidió terminar.
     */
    suspend fun listen(
        word: String,
        contest: Boolean,
        onReady: () -> Unit = {},
        stopRequested: () -> Boolean = { false }
    ): Capture = withContext(Dispatchers.IO) {
        val m = model ?: return@withContext Capture(emptyList(), emptyList())
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        var record: AudioRecord? = null
        var forcedRec: Recognizer? = null
        var freeRec: Recognizer? = null
        val forced = ArrayList<TimedToken>()
        val free = ArrayList<TimedToken>()
        try {
            val phrases = if (contest) PhraseGrammar.contest(word) else PhraseGrammar.training(word)
            // Primero se arman las gramáticas y luego se empieza a grabar, para no perder el inicio.
            forcedRec = Recognizer(m, sampleRate.toFloat(), PhraseGrammar.toJson(phrases))
            forcedRec.setWords(true)
            freeRec = Recognizer(m, sampleRate.toFloat(), freeGrammar)
            freeRec.setWords(true)
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf, 8000) * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) return@withContext Capture(emptyList(), emptyList())
            record.startRecording()
            onReady()

            val buf = ShortArray(1600)
            val start = System.currentTimeMillis()
            var lastVoice = start
            var spoke = false
            while (isActive) {
                val n = record.read(buf, 0, buf.size)
                if (n <= 0) continue
                var sum = 0.0
                for (i in 0 until n) sum += buf[i].toDouble() * buf[i]
                val rms = sqrt(sum / n)
                val now = System.currentTimeMillis()
                if (rms > VOICE_RMS) { lastVoice = now; spoke = true }
                if (forcedRec.acceptWaveForm(buf, n)) forced += parse(forcedRec.result)
                if (freeRec.acceptWaveForm(buf, n)) free += parse(freeRec.result)
                if (stopRequested()) break
                if (spoke && now - lastVoice > END_SILENCE_MS) break
                if (!spoke && now - start > NO_SPEECH_MS) break
                if (now - start > MAX_MS) break
            }
            forced += parse(forcedRec.finalResult)
            free += parse(freeRec.finalResult)
        } catch (e: Throwable) {
            // Sin permiso o sin micrófono: se devuelve lo que haya.
        } finally {
            try { record?.stop() } catch (_: Throwable) {}
            try { record?.release() } catch (_: Throwable) {}
            try { forcedRec?.close() } catch (_: Throwable) {}
            try { freeRec?.close() } catch (_: Throwable) {}
        }
        Capture(forced, free)
    }

    private fun parse(json: String): List<TimedToken> {
        val o = JSONObject(json)
        val arr = o.optJSONArray("result") ?: return emptyList()
        val out = ArrayList<TimedToken>()
        for (i in 0 until arr.length()) {
            val w = arr.getJSONObject(i)
            out += TimedToken(
                w.optString("word"), w.optDouble("conf", 1.0).toFloat(),
                w.optDouble("start", 0.0), w.optDouble("end", 0.0)
            )
        }
        return out
    }

    companion object {
        private const val MODEL_VERSION = 1
        private const val VOICE_RMS = 700.0
        private const val END_SILENCE_MS = 1700L
        private const val NO_SPEECH_MS = 9000L
        private const val MAX_MS = 28000L
    }
}
