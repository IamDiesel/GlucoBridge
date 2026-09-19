package de.glucobridge.core.model

/** Anzeige-Einheit (FR-G6). Intern wird immer in mg/dL gerechnet. */
enum class GlucoseUnit { MG_DL, MMOL_L }

/** Trendrichtung (FR-G2). */
enum class Trend { RISING_QUICK, RISING, STABLE, FALLING, FALLING_QUICK, UNKNOWN }

/** Lage relativ zum Zielbereich (FR-G4). */
enum class Zone { LOW, IN_RANGE, HIGH }

/** Frei einstellbarer Zielbereich, Default 70–180 mg/dL (FR-G4). */
data class TargetRange(val lowMgDl: Int = 70, val highMgDl: Int = 180) {
    fun zoneFor(valueMgDl: Int): Zone = when {
        valueMgDl < lowMgDl -> Zone.LOW
        valueMgDl > highMgDl -> Zone.HIGH
        else -> Zone.IN_RANGE
    }
}

/** Ein Messwert. Bewusst plattform-neutral (Long-Zeitstempel) und serialisierbar -> Wear-tauglich. */
data class GlucoseReading(
    val valueMgDl: Int,
    val trend: Trend,
    val timestampEpochMillis: Long
) {
    fun valueIn(unit: GlucoseUnit): Double =
        if (unit == GlucoseUnit.MG_DL) valueMgDl.toDouble() else valueMgDl / 18.0182

    fun isStale(nowEpochMillis: Long, thresholdMillis: Long): Boolean =
        nowEpochMillis - timestampEpochMillis > thresholdMillis
}
