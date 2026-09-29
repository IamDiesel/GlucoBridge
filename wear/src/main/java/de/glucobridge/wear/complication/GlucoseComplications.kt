package de.glucobridge.wear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountUpTimeReference
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import de.glucobridge.wear.GlucoseStore
import de.glucobridge.wear.RefreshActivity
import de.glucobridge.wear.WearRequester
import de.glucobridge.wear.arrowOf
import java.time.Instant
import java.util.concurrent.TimeUnit

private fun plain(text: String): ComplicationText = PlainComplicationText.Builder(text).build()

private fun valueGlyph(store: GlucoseStore): String =
    if (store.has()) "${store.display()}${arrowOf(store.trend)}" else "--"

private fun arrowGlyph(store: GlucoseStore): String =
    if (store.has()) arrowOf(store.trend).ifBlank { "→" } else "--"

/** Selbst-tickendes Alter ("3m", "1h") – das System haelt es ohne Push aktuell. */
private fun ageText(store: GlucoseStore): ComplicationText =
    if (!store.has() || store.ts <= 0L) plain("--")
    else TimeDifferenceComplicationText.Builder(
        TimeDifferenceStyle.SHORT_SINGLE_UNIT,
        CountUpTimeReference(Instant.ofEpochMilli(store.ts))
    ).setMinimumTimeUnit(TimeUnit.MINUTES).build()

/** Basis: SHORT_TEXT (text + optionaler title), Tap oeffnet die App. */
abstract class BaseGlucoseComplication : SuspendingComplicationDataSourceService() {
    protected abstract fun text(store: GlucoseStore): ComplicationText
    protected open fun title(store: GlucoseStore): ComplicationText? = null
    protected open fun previewText(): ComplicationText = plain("112↗")
    protected open fun previewTitle(): ComplicationText? = null

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        if (type == ComplicationType.SHORT_TEXT) build(previewText(), previewTitle()) else null

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        // On-Demand: beim Rendern der Complication einen frischen Wert anfordern (no-op im Push-Modus).
        WearRequester.requestIfOnDemand(this)
        val store = GlucoseStore(this)
        return build(text(store), title(store))
    }

    private fun build(text: ComplicationText, title: ComplicationText?): ShortTextComplicationData {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, RefreshActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = ShortTextComplicationData.Builder(
            text = text,
            contentDescription = plain("Glukose")
        ).setTapAction(tap)
        if (title != null) b.setTitle(title)
        return b.build()
    }
}

/** Nur Wert (+Pfeil). */
class ValueComplicationService : BaseGlucoseComplication() {
    override fun text(store: GlucoseStore) = plain(valueGlyph(store))
    override fun previewText() = plain("112↗")
}

/** Nur Alter (+Pfeil im Titel). */
class AgeComplicationService : BaseGlucoseComplication() {
    override fun text(store: GlucoseStore) = ageText(store)
    override fun title(store: GlucoseStore): ComplicationText? = if (store.has()) plain(arrowGlyph(store)) else null
    override fun previewText() = plain("3m")
    override fun previewTitle() = plain("↗")
}

/** Wert (+Pfeil) im Text, Alter im Titel. */
class ValueAgeComplicationService : BaseGlucoseComplication() {
    override fun text(store: GlucoseStore) = plain(valueGlyph(store))
    override fun title(store: GlucoseStore): ComplicationText? = if (store.has()) ageText(store) else null
    override fun previewText() = plain("112↗")
    override fun previewTitle() = plain("3m")
}

/** Alter im Text, Wert (+Pfeil) im Titel. */
class AgeValueComplicationService : BaseGlucoseComplication() {
    override fun text(store: GlucoseStore) = ageText(store)
    override fun title(store: GlucoseStore): ComplicationText? = if (store.has()) plain(valueGlyph(store)) else null
    override fun previewText() = plain("3m")
    override fun previewTitle() = plain("112↗")
}

/** Aktualisiert alle vier Complications (aus Listener/App aufgerufen). */
internal fun requestComplicationUpdates(ctx: Context) {
    val classes = listOf(
        ValueComplicationService::class.java,
        AgeComplicationService::class.java,
        ValueAgeComplicationService::class.java,
        AgeValueComplicationService::class.java
    )
    for (cls in classes) {
        runCatching {
            ComplicationDataSourceUpdateRequester
                .create(ctx, ComponentName(ctx, cls))
                .requestUpdateAll()
        }
    }
}
