package de.glucobridge.app.ui

import de.glucobridge.core.model.GlucoseUnit

/** Wert in der gewaehlten Anzeige-Einheit (intern immer mg/dL). */
internal fun formatGlucose(mgdl: Int, unit: GlucoseUnit): String =
    if (unit == GlucoseUnit.MG_DL) mgdl.toString() else String.format("%.1f", mgdl / 18.0182)

internal fun unitLabel(unit: GlucoseUnit): String =
    if (unit == GlucoseUnit.MG_DL) "mg/dL" else "mmol/L"
