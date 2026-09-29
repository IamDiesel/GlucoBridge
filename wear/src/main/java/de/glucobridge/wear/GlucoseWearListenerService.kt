package de.glucobridge.wear

import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import de.glucobridge.wear.complication.requestComplicationUpdates
import de.glucobridge.wear.tile.GlucoseTileService

/** Empfaengt /glucose-Updates auch ohne offene App und frischt Tile + Complication auf. */
class GlucoseWearListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        var changed = false
        for (e in events) {
            if (e.type == DataEvent.TYPE_CHANGED && e.dataItem.uri.path == "/glucose") {
                val m = DataMapItem.fromDataItem(e.dataItem).dataMap
                GlucoseStore(this).save(
                    m.getInt("value"), m.getString("trend") ?: "UNKNOWN", m.getLong("ts"),
                    m.getInt("targetLow", 70), m.getInt("targetHigh", 180), m.getString("unit") ?: "MG_DL",
                    m.getString("mode") ?: "PUSH"
                )
                // Verlauf in den 6h-Cache der Uhr einmischen (fuer Sparkline nach Kaltstart).
                val hts = m.getLongArray("histTs") ?: LongArray(0)
                val hvl = m.getLongArray("histVal")?.let { a -> IntArray(a.size) { a[it].toInt() } } ?: IntArray(0)
                runCatching { GlucoseHistoryStore(this).merge(hts, hvl) }
                changed = true
            }
        }
        if (changed) {
            runCatching { TileService.getUpdater(this).requestUpdate(GlucoseTileService::class.java) }
            requestComplicationUpdates(this)
        }
    }
}
