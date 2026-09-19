package de.glucobridge.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import de.glucobridge.wear.GlucoseStore
import de.glucobridge.wear.MainActivity
import de.glucobridge.wear.arrowOf

/** Complication (SHORT_TEXT) mit dem aktuellen Wert fuer das Zifferblatt. */
class GlucoseComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        if (type == ComplicationType.SHORT_TEXT) shortText("116→") else null

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val store = GlucoseStore(this)
        val text = if (store.has()) "${store.display()}${arrowOf(store.trend)}" else "--"
        return shortText(text)
    }

    private fun shortText(text: String): ShortTextComplicationData {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Glukose").build()
        ).setTapAction(tap).build()
    }
}
