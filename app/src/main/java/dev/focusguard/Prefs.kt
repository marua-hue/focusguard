package dev.focusguard

import android.content.Context

object Prefs {
    /** Durée d'attente obligatoire avant de pouvoir ouvrir une appli "en pause". */
    const val WAIT_SECONDS = 10

    private const val FILE = "focusguard"
    private const val KEY_PAUSE = "pause"
    private const val KEY_NOSCROLL = "noscroll"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun get(c: Context, key: String): Set<String> =
        HashSet(sp(c).getStringSet(key, emptySet()) ?: emptySet())

    private fun set(c: Context, key: String, pkg: String, on: Boolean) {
        val s = get(c, key).toMutableSet()
        if (on) s.add(pkg) else s.remove(pkg)
        sp(c).edit().putStringSet(key, s).apply()
    }

    fun pauseSet(c: Context): Set<String> = get(c, KEY_PAUSE)
    fun noScrollSet(c: Context): Set<String> = get(c, KEY_NOSCROLL)
    fun setPause(c: Context, pkg: String, on: Boolean) = set(c, KEY_PAUSE, pkg, on)
    fun setNoScroll(c: Context, pkg: String, on: Boolean) = set(c, KEY_NOSCROLL, pkg, on)
}
