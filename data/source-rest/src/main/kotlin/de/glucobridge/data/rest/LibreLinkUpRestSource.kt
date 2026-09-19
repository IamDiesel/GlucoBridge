package de.glucobridge.data.rest

import de.glucobridge.core.api.GlucoseSource
import de.glucobridge.core.api.Session
import de.glucobridge.core.api.SourceCapabilities
import de.glucobridge.core.api.SourceException
import de.glucobridge.core.model.GlucoseReading
import de.glucobridge.core.model.Trend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Clean-REST-Quelle ueber die community-dokumentierte LibreLinkUp-Follower-API.
 * IP-frei: nur oeffentliche Endpunkte + Zugangsdaten, keine White-Box/Abbott-Artefakte.
 *
 * Diagnose-Logging via println -> landet auf Android in Logcat (Tag "System.out").
 * Filter in Android Studio Logcat: "GlucoBridge".
 * Es werden BEWUSST weder Passwort noch Token geloggt.
 */
class LibreLinkUpRestSource(
    private val client: OkHttpClient = OkHttpClient(),
    private val appVersion: String = "5.1.1"
) : GlucoseSource {

    override val capabilities = SourceCapabilities(
        id = "rest-librelinkup",
        requiresWhiteBox = false,
        survivesAleEnforcement = false
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMedia = "application/json".toMediaType()

    override suspend fun login(email: String, password: String): Session = withContext(Dispatchers.IO) {
        var base = DEFAULT_BASE
        println("$TAG login start base=$base")
        repeat(2) {
            val payload = json.encodeToString(LoginRequest.serializer(), LoginRequest(email, password))
            val raw = call("$base/llu/auth/login", session = null, postBody = payload)
            val env = json.decodeFromString(LoginEnvelope.serializer(), raw)
            val data = env.data ?: throw SourceException.AuthFailed("Unerwartete Login-Antwort (status=${env.status})")
            if (data.redirect == true && !data.region.isNullOrBlank()) {
                base = "https://api-${data.region}.libreview.io"
                println("$TAG login redirect -> region=${data.region} base=$base")
                return@repeat // erneuter Login auf dem Regions-Host
            }
            val token = data.authTicket?.token
                ?: throw SourceException.AuthFailed("Kein Token (status=${env.status}) – Zugangsdaten/AGB pruefen")
            val userId = data.user?.id ?: throw SourceException.AuthFailed("Keine User-ID in der Antwort")
            println("$TAG login OK (token laenge=${token.length}, base=$base)")
            return@withContext Session(accountId = userId, token = token, installationId = "", region = base)
        }
        throw SourceException.AuthFailed("Login fehlgeschlagen (Region-Redirect nicht aufgeloest)")
    }

    override suspend fun restore(session: Session): Session = session

    override suspend fun latest(session: Session): GlucoseReading = withContext(Dispatchers.IO) {
        val base = session.region ?: DEFAULT_BASE
        val env = json.decodeFromString(ConnectionsEnvelope.serializer(), call("$base/llu/connections", session))
        val m = env.data.firstOrNull()?.glucoseMeasurement
            ?: throw SourceException.Network("Keine Verbindung/Messung gefunden")
        m.toReading()
    }

    override suspend fun history(session: Session, hours: Int): List<GlucoseReading> = withContext(Dispatchers.IO) {
        val base = session.region ?: DEFAULT_BASE
        val conns = json.decodeFromString(ConnectionsEnvelope.serializer(), call("$base/llu/connections", session))
        val patientId = conns.data.firstOrNull()?.patientId ?: return@withContext emptyList()
        val graph = json.decodeFromString(GraphEnvelope.serializer(), call("$base/llu/connections/$patientId/graph", session))
        val cutoff = System.currentTimeMillis() - hours * 3_600_000L
        graph.data?.graphData.orEmpty().map { it.toReading() }.filter { it.timestampEpochMillis >= cutoff }
    }

    // ---- HTTP ----
    private fun call(url: String, session: Session?, postBody: String? = null): String {
        val b = Request.Builder().url(url)
        if (postBody != null) b.post(postBody.toRequestBody(jsonMedia)) else b.get()
        b.header("User-Agent", USER_AGENT)          // ohne Nicht-Bot-UA -> Cloudflare 403
        b.header("product", "llu.android")
        b.header("version", appVersion)
        b.header("Accept", "application/json")
        b.header("cache-control", "no-cache")
        if (session != null) {
            b.header("Authorization", "Bearer ${session.token}")
            b.header("Account-Id", sha256(session.accountId))
        }
        val method = if (postBody != null) "POST" else "GET"
        println("$TAG --> $method $url (auth=${session != null}, product=llu.android, version=$appVersion)")
        println("$TAG     UA=$USER_AGENT")
        client.newCall(b.build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            println("$TAG <-- ${r.code} $url")
            println("$TAG     server=${r.header("server")} cf-ray=${r.header("cf-ray")} cf-mitigated=${r.header("cf-mitigated")} content-type=${r.header("content-type")}")
            println("$TAG     body(600)=${body.take(600).replace("\n", " ").replace("\r", " ")}")
            if (!r.isSuccessful) {
                when (r.code) {
                    401, 403 ->
                        if (session == null) throw SourceException.AuthFailed("Login abgelehnt (HTTP ${r.code})")
                        else throw SourceException.SessionExpired("HTTP ${r.code}")
                    else -> throw SourceException.Network("HTTP ${r.code}")
                }
            }
            return body
        }
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun GlucoseMeasurement.toReading(): GlucoseReading = GlucoseReading(
        valueMgDl = ValueInMgPerDl ?: Value?.toInt() ?: 0,
        trend = when (TrendArrow) {
            1 -> Trend.FALLING_QUICK
            2 -> Trend.FALLING
            3 -> Trend.STABLE
            4 -> Trend.RISING
            5 -> Trend.RISING_QUICK
            else -> Trend.UNKNOWN
        },
        timestampEpochMillis = parseTs(FactoryTimestamp ?: Timestamp)
    )

    private fun parseTs(ts: String?): Long {
        if (ts.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val fmt = DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US)
            LocalDateTime.parse(ts, fmt).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    companion object {
        const val TAG = "[GlucoBridge][REST]"
        const val DEFAULT_BASE = "https://api.libreview.io"
        // Bewaehrt (funktionierender Python-Client): API erwartet den okhttp-UA, KEIN Browser-UA.
        const val USER_AGENT = "okhttp/4.10.0"
    }
}

// ---- DTOs (Server-Feldnamen exakt; unbekannte Felder werden ignoriert) ----
@Serializable private data class LoginRequest(val email: String, val password: String)
@Serializable private data class LoginEnvelope(val status: Int? = null, val data: LoginData? = null)
@Serializable private data class LoginData(
    val redirect: Boolean? = null,
    val region: String? = null,
    val authTicket: AuthTicket? = null,
    val user: User? = null
)
@Serializable private data class AuthTicket(val token: String? = null)
@Serializable private data class User(val id: String? = null)
@Serializable private data class ConnectionsEnvelope(val data: List<Connection> = emptyList())
@Serializable private data class Connection(
    val patientId: String? = null,
    val glucoseMeasurement: GlucoseMeasurement? = null
)
@Serializable private data class GlucoseMeasurement(
    val ValueInMgPerDl: Int? = null,
    val Value: Double? = null,
    val TrendArrow: Int? = null,
    val FactoryTimestamp: String? = null,
    val Timestamp: String? = null
)
@Serializable private data class GraphEnvelope(val data: GraphData? = null)
@Serializable private data class GraphData(
    val connection: Connection? = null,
    val graphData: List<GlucoseMeasurement> = emptyList()
)
