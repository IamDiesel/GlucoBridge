package de.glucobridge.app.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import de.glucobridge.domain.GlucoseRepository
import de.glucobridge.domain.GlucoseState
import de.glucobridge.domain.Settings
import de.glucobridge.domain.WearMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Schiebt den jeweils aktuellen Messwert per Data Layer an die Uhr (Pfad /glucose). */
class WearSyncer(
    context: Context,
    private val repo: GlucoseRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)

    init {
        scope.launch {
            var lastKey = ""
            var lastMode: WearMode? = null
            combine(repo.state, repo.settings) { st, se -> st to se }.collect { (st, se) ->
                val content = st as? GlucoseState.Content ?: return@collect
                val r = content.reading
                val key = "${r.valueMgDl}|${r.timestampEpochMillis}|" +
                    "${se.target.lowMgDl}|${se.target.highMgDl}|${se.unit.name}"
                val modeChanged = se.wearMode != lastMode
                lastMode = se.wearMode
                val isLow = r.valueMgDl < se.target.lowMgDl
                // PUSH: bei jeder echten Aenderung. Unterzucker: IMMER sofort (auch unveraendert).
                // ON_DEMAND: nur beim Moduswechsel spiegeln, sonst auf Uhr-Anfrage warten (aber Low trotzdem).
                val shouldPush = when {
                    modeChanged -> true
                    isLow -> true
                    se.wearMode == WearMode.PUSH && key != lastKey -> true
                    else -> false
                }
                if (shouldPush) { lastKey = key; push(content, se) }
            }
        }
    }

    /** Aktuellen Wert + Verlauf sofort an die Uhr senden (Antwort auf eine On-Demand-Anfrage). */
    fun pushCurrent() {
        val content = repo.state.value as? GlucoseState.Content ?: return
        push(content, repo.settings.value)
    }

    private fun push(content: GlucoseState.Content, se: Settings) {
        val r = content.reading
        // Kompaktes Verlaufsfenster fuer die Uhr (chronologisch, gedeckelt -> kleine Payload).
        val hist = content.history.sortedBy { it.timestampEpochMillis }.takeLast(240)
        runCatching {
            val req = PutDataMapRequest.create(PATH).apply {
                dataMap.putInt("value", r.valueMgDl)
                dataMap.putString("trend", r.trend.name)
                dataMap.putLong("ts", r.timestampEpochMillis)
                dataMap.putInt("targetLow", se.target.lowMgDl)
                dataMap.putInt("targetHigh", se.target.highMgDl)
                dataMap.putString("unit", se.unit.name)
                dataMap.putString("mode", se.wearMode.name)
                // Nonce -> Bytes aendern sich immer, onDataChanged feuert auch bei gleichem Wert (Cache-Antwort).
                dataMap.putLong("pushedAt", System.currentTimeMillis())
                dataMap.putLongArray("histTs", LongArray(hist.size) { hist[it].timestampEpochMillis })
                dataMap.putLongArray("histVal", LongArray(hist.size) { hist[it].valueMgDl.toLong() })
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(req)
        }
    }

    companion object { const val PATH = "/glucose" }
}
