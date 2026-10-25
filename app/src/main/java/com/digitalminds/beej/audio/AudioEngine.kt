package com.digitalminds.beej.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.digitalminds.beej.core.AudioStep
import com.digitalminds.beej.core.Speed
import com.digitalminds.beej.core.SpellPlanner
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * Reproduce los audios incluidos en el APK (carpeta assets/audio, archivos .ogg) sin internet.
 * Cada letra es un audio aparte, así controlamos la pausa entre letras.
 */
class AudioEngine(private val context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .build()

    private val soundIds = ConcurrentHashMap<String, Int>()
    private val loadedIds = ConcurrentHashMap.newKeySet<Int>()
    private val activeStreams = ConcurrentHashMap.newKeySet<Int>()
    private val durations: Map<String, Int> = readManifest()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loadedIds.add(id) }
    }

    private fun readManifest(): Map<String, Int> = try {
        context.assets.open("audio/manifest.txt").bufferedReader().useLines { lines ->
            lines.mapNotNull { l ->
                val p = l.trim().split('=')
                if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: 700) else null
            }.toMap()
        }
    } catch (e: Exception) {
        emptyMap()
    }

    private fun load(file: String): Int? {
        soundIds[file]?.let { return it }
        return try {
            context.assets.openFd("audio/$file.ogg").use { afd ->
                val id = pool.load(afd, 1)
                soundIds[file] = id
                id
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Carga por adelantado para que el audio salga casi al instante. */
    fun preload(files: Collection<String>) {
        for (f in files) load(f)
    }

    private suspend fun ready(file: String): Int? {
        val id = load(file) ?: return null
        var waited = 0
        while (id !in loadedIds && waited < 1500) {
            delay(5); waited += 5
        }
        return if (id in loadedIds) id else null
    }

    /** Reproduce un audio y espera a que termine. */
    suspend fun play(file: String) {
        val id = ready(file) ?: return
        val stream = pool.play(id, 1f, 1f, 1, 0, 1f)
        if (stream != 0) activeStreams.add(stream)
        delay((durations[file] ?: 700).toLong())
    }

    suspend fun playWord(word: String, speed: Speed) {
        play(SpellPlanner.fileFor(SpellPlanner.wordClip(word), speed))
    }

    /**
     * Reproduce una secuencia (deletreo). [onStep] avisa el índice del paso que empieza.
     * Si la corrutina se cancela, se corta el sonido.
     */
    suspend fun playSteps(steps: List<AudioStep>, speed: Speed, onStep: (Int) -> Unit = {}) {
        try {
            steps.forEachIndexed { i, s ->
                onStep(i)
                val file = SpellPlanner.fileFor(s.clip, speed)
                val id = ready(file)
                if (id != null) {
                    val stream = pool.play(id, 1f, 1f, 1, 0, 1f)
                    if (stream != 0) activeStreams.add(stream)
                }
                delay(((durations[file] ?: 600) + s.gapAfterMs).toLong())
            }
        } finally {
            onStep(-1)
        }
    }

    fun stopAll() {
        for (s in activeStreams) pool.stop(s)
        activeStreams.clear()
    }

    fun release() {
        stopAll()
        pool.release()
    }

    companion object {
        fun filesForWordSpeed(speed: Speed, words: List<String>): List<String> =
            words.map { SpellPlanner.fileFor(SpellPlanner.wordClip(it), speed) }

        fun filesForLetterSpeed(speed: Speed): List<String> =
            ('a'..'z').map { SpellPlanner.fileFor("l_$it", speed) } +
                SpellPlanner.fileFor(SpellPlanner.DOUBLE_CLIP, speed)
    }
}
