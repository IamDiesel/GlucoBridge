package de.glucobridge.data.rest

import de.glucobridge.core.api.GlucoseSource
import de.glucobridge.core.api.GlucoseSourceProvider
import de.glucobridge.core.api.Session
import de.glucobridge.core.api.SourceCapabilities
import de.glucobridge.core.model.GlucoseReading
import de.glucobridge.core.model.Trend
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * M0-Platzhalter der Clean-REST-Quelle: liefert Beispiel-Daten, damit die Architektur
 * vertikal lauffaehig ist. In M1 durch den echten Retrofit/OkHttp-Client ersetzt
 * (community-dokumentierte LibreLinkUp-REST-API, IP-frei).
 */
class FakeRestGlucoseSource : GlucoseSource {
    override val capabilities = SourceCapabilities(
        id = "rest-fake",
        requiresWhiteBox = false,
        survivesAleEnforcement = false
    )

    override suspend fun login(email: String, password: String): Session {
        delay(300)
        return Session(accountId = "demo", token = "demo-token", installationId = "demo-iid", region = "de")
    }

    override suspend fun restore(session: Session): Session = session

    override suspend fun latest(session: Session): GlucoseReading {
        delay(150)
        return GlucoseReading(valueMgDl = 139, trend = Trend.STABLE, timestampEpochMillis = System.currentTimeMillis())
    }

    override suspend fun history(session: Session, hours: Int): List<GlucoseReading> {
        val now = System.currentTimeMillis()
        val step = 5 * 60_000L
        return (0 until hours * 12).map { i ->
            GlucoseReading(
                valueMgDl = 120 + (sin(i / 6.0) * 30).toInt(),
                trend = Trend.STABLE,
                timestampEpochMillis = now - i * step
            )
        }.reversed()
    }

    companion object {
        val provider = GlucoseSourceProvider { FakeRestGlucoseSource() }
    }
}
