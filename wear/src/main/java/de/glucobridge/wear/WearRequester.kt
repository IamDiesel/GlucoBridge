package de.glucobridge.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable

/**
 * Uhr-Seite des On-Demand-Modus: schickt beim Hinschauen (App/Tile/Complication) eine Anfrage
 * ans Handy – aber nur wenn der aktuelle Modus ON_DEMAND ist und nicht oefter als alle 30 s.
 */
object WearRequester {
    private const val PATH = "/glucose-request"
    private const val MIN_INTERVAL_MS = 30_000L
    private const val TAP_DEBOUNCE_MS = 3_000L
    @Volatile private var lastSent = 0L

    /** On-Demand-Modus: beim Hinschauen anfragen (gedrosselt auf 30 s). */
    fun requestIfOnDemand(context: Context) {
        if (GlucoseStore(context.applicationContext).mode != "ON_DEMAND") return
        val now = System.currentTimeMillis()
        if (now - lastSent < MIN_INTERVAL_MS) return
        lastSent = now
        send(context.applicationContext)
    }

    /** Manuell (Tippen auf Complication): immer anfragen, nur Doppel-Tap entprellen. */
    fun requestNow(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastSent < TAP_DEBOUNCE_MS) return
        lastSent = now
        send(context.applicationContext)
    }

    private fun send(ctx: Context) {
        runCatching {
            Wearable.getNodeClient(ctx).connectedNodes.addOnSuccessListener { nodes ->
                val mc = Wearable.getMessageClient(ctx)
                for (n in nodes) runCatching { mc.sendMessage(n.id, PATH, ByteArray(0)) }
            }
        }
    }
}
