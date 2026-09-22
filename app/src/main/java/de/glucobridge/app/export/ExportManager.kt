package de.glucobridge.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import de.glucobridge.core.model.GlucoseReading
import de.glucobridge.domain.HistoryStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Liest den Verlauf, schreibt eine Export-Datei in den Cache und baut einen Share-Intent. */
class ExportManager(
    private val appContext: Context,
    private val historyStore: HistoryStore
) {
    enum class Format(val ext: String, val mime: String) {
        CSV("csv", "text/csv"),
        FHIR("json", "application/fhir+json"),
        NIGHTSCOUT("json", "application/json")
    }

    suspend fun readings(rangeHours: Int): List<GlucoseReading> =
        historyStore.since(System.currentTimeMillis() - rangeHours * 3_600_000L)

    fun format(fmt: Format, readings: List<GlucoseReading>): String = when (fmt) {
        Format.CSV -> GlucoseExporters.csv(readings)
        Format.FHIR -> GlucoseExporters.fhirBundle(readings)
        Format.NIGHTSCOUT -> GlucoseExporters.nightscoutEntries(readings)
    }

    /** Schreibt den Inhalt in cache/exports und liefert einen ACTION_SEND-Intent (FileProvider). */
    fun shareIntent(fmt: Format, content: String): Intent {
        val dir = File(appContext.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val suffix = if (fmt == Format.NIGHTSCOUT) "nightscout" else fmt.name.lowercase()
        val file = File(dir, "glucobridge_${stamp}_$suffix.${fmt.ext}")
        file.writeText(content)
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = fmt.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
