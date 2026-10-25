package com.digitalminds.beej.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.digitalminds.beej.core.HeardToken
import com.digitalminds.beej.core.SpokenLetters
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

    private val grammar: String by lazy {
        (SpokenLetters.grammarTokens() + "[unk]").joinToString(",", "[", "]") { "\"$it\"" }
    }

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
     * @param onPartial texto parcial en vivo.
     * @param stopRequested devuelve true si el usuario pidió terminar.
     */
    suspend fun listen(
        onPartial: (String) -> Unit = {},
        stopRequested: () -> Boolean = { false }
    ): List<HeardToken> = withContext(Dispatchers.IO) {
        val m = model ?: return@withContext emptyList()
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        var record: AudioRecord? = null
        var rec: Recognizer? = null
        val collected = ArrayList<HeardToken>()
        try {
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf, 8000) * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) return@withContext emptyList()
            rec = Recognizer(m, sampleRate.toFloat(), grammar)
            rec.setWords(true)
            record.startRecording()

            val buf = ShortArray(1600)
            val start = System.currentTimeMillis()
            var lastVoice = start
            var spoke = false
            var lastPartial = ""
            while (isActive) {
                val n = record.read(buf, 0, buf.size)
                if (n <= 0) continue
                var sum = 0.0
                for (i in 0 until n) sum += buf[i].toDouble() * buf[i]
                val rms = sqrt(sum / n)
                val now = System.currentTimeMillis()
                if (rms > VOICE_RMS) { lastVoice = now; spoke = true }
                if (rec.acceptWaveForm(buf, n)) {
                    collected += parse(rec.result)
                } else {
                    val p = JSONObject(rec.partialResult).optString("partial", "")
                    if (p != lastPartial) { lastPartial = p; onPartial(p) }
                }
                val silent = now - lastVoice
                if (stopRequested()) break
                if (spoke && silent > END_SILENCE_MS) break
                if (!spoke && now - start > NO_SPEECH_MS) break
                if (now - start > MAX_MS) break
            }
            collected += parse(rec.finalResult)
        } catch (e: Throwable) {
            // Sin permiso o sin micrófono: se devuelve lo que haya.
        } finally {
            try { record?.stop() } catch (_: Throwable) {}
            try { record?.release() } catch (_: Throwable) {}
            try { rec?.close() } catch (_: Throwable) {}
        }
        collected
    }

    private fun parse(json: String): List<HeardToken> {
        val o = JSONObject(json)
        val arr = o.optJSONArray("result")
        if (arr != null) {
            val out = ArrayList<HeardToken>()
            for (i in 0 until arr.length()) {
                val w = arr.getJSONObject(i)
                out += HeardToken(w.optString("word"), w.optDouble("conf", 1.0).toFloat())
            }
            return out
        }
        return o.optString("text", "").split(' ').filter { it.isNotBlank() }.map { HeardToken(it) }
    }

    companion object {
        private const val MODEL_VERSION = 1
        private const val VOICE_RMS = 700.0
        private const val END_SILENCE_MS = 1700L
        private const val NO_SPEECH_MS = 9000L
        private const val MAX_MS = 28000L
    }
}
