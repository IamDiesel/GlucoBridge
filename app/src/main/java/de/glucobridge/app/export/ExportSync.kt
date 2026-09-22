package de.glucobridge.app.export

import de.glucobridge.app.data.ExportSettingsStore
import de.glucobridge.domain.GlucoseRepository
import de.glucobridge.domain.GlucoseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Schiebt jeden neuen Messwert automatisch an die aktivierten Ziele (Health Connect, Nightscout).
 * Dedup per Zeitstempel; Backfill ganzer Zeitraeume laeuft ueber die manuellen Buttons.
 */
class ExportSync(
    repo: GlucoseRepository,
    private val store: ExportSettingsStore,
    private val hc: HealthConnectExporter,
    private val ns: NightscoutUploader,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    @Volatile private var lastHc = 0L
    @Volatile private var lastNs = 0L

    init {
        scope.launch {
            repo.state.collect { st ->
                val r = (st as? GlucoseState.Content)?.reading ?: return@collect
                val cfg = store.load()
                if (cfg.healthConnectEnabled && r.timestampEpochMillis > lastHc) {
                    runCatching {
                        if (hc.hasPermission()) { hc.write(listOf(r)); lastHc = r.timestampEpochMillis }
                    }
                }
                if (cfg.nightscoutEnabled && cfg.nightscoutUrl.isNotBlank() && r.timestampEpochMillis > lastNs) {
                    ns.upload(cfg.nightscoutUrl, store.nightscoutSecret, listOf(r))
                        .onSuccess { lastNs = r.timestampEpochMillis }
                }
            }
        }
    }
}
