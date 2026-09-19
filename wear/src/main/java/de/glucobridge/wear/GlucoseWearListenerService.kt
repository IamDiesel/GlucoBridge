package de.glucobridge.wear

import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import de.glucobridge.wear.complication.GlucoseComplicationService
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
                    m.getInt("targetLow", 70), m.getInt("targetHigh", 180), m.getString("unit") ?: "MG_DL"
                )
                changed = true
            }
        }
        if (changed) {
            runCatching { TileService.getUpdater(this).requestUpdate(GlucoseTileService::class.java) }
            runCatching {
                ComplicationDataSourceUpdateRequester
                    .create(this, ComponentName(this, GlucoseComplicationService::class.java))
                    .requestUpdateAll()
            }
        }
    }
}
