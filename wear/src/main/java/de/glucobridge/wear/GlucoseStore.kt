package de.glucobridge.wear

import android.content.Context
import de.glucobridge.core.model.GlucoseUnit
import de.glucobridge.core.model.TargetRange
import de.glucobridge.core.model.Trend
import de.glucobridge.core.model.Zone

/** Lokaler Uhr-Cache des zuletzt gepushten Werts (fuer Tile/Complication/App). */
class GlucoseStore(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("glucose_wear", Context.MODE_PRIVATE)

    fun save(value: Int, trend: String, ts: Long, low: Int, high: Int, unit: String) {
        p.edit().putInt("value", value).putString("trend", trend).putLong("ts", ts)
            .putInt("low", low).putInt("high", high).putString("unit", unit).apply()
    }

    val value: Int get() = p.getInt("value", -1)
    val trend: String get() = p.getString("trend", "UNKNOWN") ?: "UNKNOWN"
    val ts: Long get() = p.getLong("ts", 0L)
    val low: Int get() = p.getInt("low", 70)
    val high: Int get() = p.getInt("high", 180)
    val unit: String get() = p.getString("unit", "MG_DL") ?: "MG_DL"

    fun has(): Boolean = value >= 0
    fun zone(): Zone = TargetRange(low, high).zoneFor(value)
    fun display(): String =
        if (unit == GlucoseUnit.MMOL_L.name) String.format("%.1f", value / 18.0182) else value.toString()
    fun unitLabel(): String = if (unit == GlucoseUnit.MMOL_L.name) "mmol/L" else "mg/dL"
}

internal fun arrowOf(trend: String): String =
    when (runCatching { Trend.valueOf(trend) }.getOrDefault(Trend.UNKNOWN)) {
        Trend.RISING_QUICK -> "↑"
        Trend.RISING -> "↗"
        Trend.STABLE -> "→"
        Trend.FALLING -> "↘"
        Trend.FALLING_QUICK -> "↓"
        Trend.UNKNOWN -> ""
    }

internal fun zoneColorInt(zone: Zone): Int = when (zone) {
    Zone.LOW -> 0xFFEF5350.toInt()
    Zone.HIGH -> 0xFFFFCA28.toInt()
    Zone.IN_RANGE -> 0xFF66BB6A.toInt()
}

internal fun ageShort(ts: Long): String {
    val m = (System.currentTimeMillis() - ts) / 60_000L
    return when {
        m < 1 -> "jetzt"
        m < 60 -> "vor $m min"
        else -> "vor ${m / 60} h"
    }
}
