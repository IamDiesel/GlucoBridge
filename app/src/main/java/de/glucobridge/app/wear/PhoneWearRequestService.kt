package de.glucobridge.app.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import de.glucobridge.app.GlucoBridgeApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Handy-Seite des On-Demand-Modus: die Uhr fragt beim Hinschauen unter /glucose-request an.
 * Wir holen hoechstens einmal pro Minute frisch vom Server (sonst Cache) und pushen das Ergebnis
 * ueber den bestehenden /glucose-Kanal zurueck an die Uhr.
 */
class PhoneWearRequestService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != PATH_REQUEST) return
        val container = (applicationContext as GlucoBridgeApp).container
        val repo = container.glucoseRepository
        val syncer = container.wearSyncer

        val now = System.currentTimeMillis()
        val doFetch = now - lastFetchAt >= MIN_FETCH_INTERVAL_MS
        if (doFetch) {
            lastFetchAt = now
            // Blockiert diesen Binder-Thread kurz, damit der Service bis zur Antwort lebt.
            runBlocking { withTimeoutOrNull(FETCH_TIMEOUT_MS) { runCatching { repo.refreshNow() } } }
        }
        // Frischen Wert oder (bei Rate-Limit) den Cache an die Uhr schicken.
        syncer.pushCurrent()
    }

    companion object {
        const val PATH_REQUEST = "/glucose-request"
        private const val MIN_FETCH_INTERVAL_MS = 60_000L
        private const val FETCH_TIMEOUT_MS = 12_000L
        @Volatile private var lastFetchAt = 0L
    }
}
