package com.digitalminds.beej.data

import android.content.Context
import com.digitalminds.beej.core.Progress
import com.digitalminds.beej.core.ProgressCodec
import com.digitalminds.beej.core.Speed
import com.digitalminds.beej.core.SpellSpeed

/** Todo se guarda en el teléfono (SharedPreferences). Sin cuentas, sin internet. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("beej", Context.MODE_PRIVATE)

    fun loadProgress(): Progress = ProgressCodec.decode(prefs.getString("progress", null))
    fun saveProgress(p: Progress) { prefs.edit().putString("progress", ProgressCodec.encode(p)).apply() }

    var wordSpeed: Speed
        get() = Speed.fromOrdinal(prefs.getInt("word_speed", Speed.NORMAL.ordinal))
        set(v) { prefs.edit().putInt("word_speed", v.ordinal).apply() }

    var spellSpeed: SpellSpeed
        get() = SpellSpeed.fromOrdinal(prefs.getInt("spell_speed2", SpellSpeed.NORMAL.ordinal))
        set(v) { prefs.edit().putInt("spell_speed2", v.ordinal).apply() }

    var doubleMode: Boolean
        get() = prefs.getBoolean("double_mode", false)
        set(v) { prefs.edit().putBoolean("double_mode", v).apply() }

    var bestContest: Int
        get() = prefs.getInt("best_contest", 0)
        set(v) { prefs.edit().putInt("best_contest", v).apply() }
}
