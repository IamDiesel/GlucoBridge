package de.glucobridge.app.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import de.glucobridge.domain.GlucoseRepository
import de.glucobridge.domain.GlucoseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Schiebt den jeweils aktuellen Messwert per Data Layer an die Uhr (Pfad /glucose). */
class WearSyncer(
    context: Context,
    repo: GlucoseRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)

    init {
        scope.launch {
            combine(repo.state, repo.settings) { st, se -> st to se }.collect { (st, se) ->
                val r = (st as? GlucoseState.Content)?.reading ?: return@collect
                runCatching {
                    val req = PutDataMapRequest.create(PATH).apply {
                        dataMap.putInt("value", r.valueMgDl)
                        dataMap.putString("trend", r.trend.name)
                        dataMap.putLong("ts", r.timestampEpochMillis)
                        dataMap.putInt("targetLow", se.target.lowMgDl)
                        dataMap.putInt("targetHigh", se.target.highMgDl)
                        dataMap.putString("unit", se.unit.name)
                    }.asPutDataRequest().setUrgent()
                    dataClient.putDataItem(req)
                }
            }
        }
    }

    companion object { const val PATH = "/glucose" }
}
