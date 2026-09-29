package de.glucobridge.wear

import android.content.Context

/** Persistenter Verlaufscache der Uhr (6-h-Fenster). Merge nach Zeitstempel + Prune nach Alter. */
class GlucoseHistoryStore(context: Context) {
    private val p = context.applicationContext
        .getSharedPreferences("glucose_wear_hist", Context.MODE_PRIVATE)

    /** Neue Punkte einmischen: dedupliziert nach ts, prunt >6h, deckelt die Menge. */
    fun merge(ts: LongArray, values: IntArray) {
        if (ts.isEmpty() || ts.size != values.size) return
        val map = HashMap<Long, Int>()
        val (ots, ovs) = load()
        for (i in ots.indices) map[ots[i]] = ovs[i]
        for (i in ts.indices) map[ts[i]] = values[i]
        val cutoff = System.currentTimeMillis() - WINDOW_MS
        val entries = map.entries.filter { it.key >= cutoff }.sortedBy { it.key }.takeLast(MAX_POINTS)
        val sb = StringBuilder()
        for (e in entries) {
            if (sb.isNotEmpty()) sb.append(';')
            sb.append(e.key).append(':').append(e.value)
        }
        p.edit().putString("pts", sb.toString()).apply()
    }

    /** Aktueller Cache, bereits auf 6h gefiltert (chronologisch). */
    fun load(): Pair<LongArray, IntArray> {
        val raw = p.getString("pts", "") ?: ""
        if (raw.isEmpty()) return LongArray(0) to IntArray(0)
        val cutoff = System.currentTimeMillis() - WINDOW_MS
        val ts = ArrayList<Long>(); val vs = ArrayList<Int>()
        for (tok in raw.split(';')) {
            val c = tok.indexOf(':'); if (c <= 0) continue
            val t = tok.substring(0, c).toLongOrNull() ?: continue
            val v = tok.substring(c + 1).toIntOrNull() ?: continue
            if (t >= cutoff) { ts.add(t); vs.add(v) }
        }
        return LongArray(ts.size) { ts[it] } to IntArray(vs.size) { vs[it] }
    }

    fun clear() { p.edit().remove("pts").apply() }

    companion object {
        private const val WINDOW_MS = 6L * 60 * 60 * 1000  // 6 Stunden
        private const val MAX_POINTS = 400
    }
}
