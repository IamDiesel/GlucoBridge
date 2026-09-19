package de.glucobridge.core.api

import de.glucobridge.core.model.GlucoseReading

/** Opaques Sitzungs-Handle einer Datenquelle (Token/Installations-ID etc.). */
data class Session(
    val accountId: String,
    val token: String,
    val installationId: String,
    val region: String? = null,
    /** Generischer, quellen-spezifischer Blob (z. B. ALE-Key-Package kp). IP-frei. */
    val opaque: String? = null
)

/** Selbstauskunft einer Quelle (fuer Auswahl/Fallback und UI-Hinweise). */
data class SourceCapabilities(
    val id: String,
    val requiresWhiteBox: Boolean,
    val survivesAleEnforcement: Boolean
)

/** Einheitliche Fehlerarten aller Quellen. */
sealed class SourceException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class AuthFailed(message: String = "Login fehlgeschlagen") : SourceException(message)
    class SessionExpired(message: String = "Sitzung abgelaufen") : SourceException(message)
    class Network(message: String = "Netzwerkfehler", cause: Throwable? = null) : SourceException(message, cause)
    class Unavailable(message: String = "Datenquelle nicht verfuegbar") : SourceException(message)
}

/**
 * Stabile Schnittstelle (SPI) zwischen App und einer konkreten Glukose-Quelle.
 * Implementierungen: :data:source-rest (committet) und :data:source-ale (IP-Zone, git-ignoriert).
 * Diese Datei enthaelt KEIN IP-Wissen.
 */
interface GlucoseSource {
    val capabilities: SourceCapabilities
    suspend fun login(email: String, password: String): Session
    suspend fun restore(session: Session): Session
    suspend fun latest(session: Session): GlucoseReading
    suspend fun history(session: Session, hours: Int): List<GlucoseReading>
}

/** Provider einer Quelle; liefert null, wenn die Quelle nicht einsatzbereit ist. */
fun interface GlucoseSourceProvider {
    fun create(): GlucoseSource?
}
