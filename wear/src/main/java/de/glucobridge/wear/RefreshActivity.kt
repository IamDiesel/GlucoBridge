package de.glucobridge.wear

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/**
 * Unsichtbare Aktivitaet: wird beim Tippen auf eine Complication gestartet, fordert einen
 * frischen Wert an (ueber den /glucose-request-Kanal ans Handy) und beendet sich sofort.
 * Kein sichtbares UI -> der Nutzer bleibt auf dem Zifferblatt.
 */
class RefreshActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { WearRequester.requestNow(this) }
        runCatching { Toast.makeText(this, "Aktualisiere\u2026", Toast.LENGTH_SHORT).show() }
        finish()
    }
}
