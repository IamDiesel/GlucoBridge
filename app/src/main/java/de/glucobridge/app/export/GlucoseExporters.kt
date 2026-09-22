package de.glucobridge.app.export

import de.glucobridge.core.model.GlucoseReading
import de.glucobridge.core.model.Trend
import java.time.Instant

/**
 * Reine Formatierer (kein I/O). Glukose wird in mg/dL exportiert (Standard fuer LOINC 2339-0
 * und Nightscout sgv). Eingaben sind ausschliesslich Zahlen/feste Strings -> kein JSON-Escaping noetig.
 */
object GlucoseExporters {

    private fun iso(ts: Long): String = Instant.ofEpochMilli(ts).toString()  // UTC ISO-8601

    fun csv(readings: List<GlucoseReading>): String = buildString {
        append("timestamp_iso,epoch_ms,mg_dl,trend\n")
        for (r in readings) {
            append(iso(r.timestampEpochMillis)).append(',')
                .append(r.timestampEpochMillis).append(',')
                .append(r.valueMgDl).append(',')
                .append(r.trend.name).append('\n')
        }
    }

    /** HL7 FHIR R4 Bundle (collection) aus Observation-Ressourcen (LOINC 2339-0, mg/dL). */
    fun fhirBundle(readings: List<GlucoseReading>): String = buildString {
        append("{\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\":[")
        readings.forEachIndexed { i, r ->
            if (i > 0) append(',')
            append("{\"resource\":{")
            append("\"resourceType\":\"Observation\",\"status\":\"final\",")
            append("\"category\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/observation-category\",\"code\":\"laboratory\"}]}],")
            append("\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"2339-0\",\"display\":\"Glucose [Mass/volume] in Blood\"}]},")
            append("\"effectiveDateTime\":\"").append(iso(r.timestampEpochMillis)).append("\",")
            append("\"valueQuantity\":{\"value\":").append(r.valueMgDl)
            append(",\"unit\":\"mg/dL\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"mg/dL\"}")
            append("}}")
        }
        append("]}")
    }

    /** Nightscout `entries` (sgv). direction gemappt aus dem Trend. */
    fun nightscoutEntries(readings: List<GlucoseReading>): String = buildString {
        append('[')
        readings.forEachIndexed { i, r ->
            if (i > 0) append(',')
            append("{\"type\":\"sgv\",\"sgv\":").append(r.valueMgDl)
            append(",\"date\":").append(r.timestampEpochMillis)
            append(",\"dateString\":\"").append(iso(r.timestampEpochMillis)).append("\"")
            append(",\"direction\":\"").append(nsDirection(r.trend)).append("\"")
            append(",\"device\":\"GlucoBridge\"}")
        }
        append(']')
    }

    private fun nsDirection(t: Trend): String = when (t) {
        Trend.RISING_QUICK -> "DoubleUp"
        Trend.RISING -> "SingleUp"
        Trend.STABLE -> "Flat"
        Trend.FALLING -> "SingleDown"
        Trend.FALLING_QUICK -> "DoubleDown"
        Trend.UNKNOWN -> "NONE"
    }
}
