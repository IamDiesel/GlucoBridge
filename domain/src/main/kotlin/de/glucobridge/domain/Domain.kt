package de.glucobridge.domain

import de.glucobridge.core.api.Session
import de.glucobridge.core.model.GlucoseReading
import de.glucobridge.core.model.GlucoseUnit
import de.glucobridge.core.model.TargetRange
import kotlinx.coroutines.flow.StateFlow

/** Bevorzugte Datenquelle (FR-P7): ALE als Default, REST als Rueckfallebene. */
enum class DataSourceMode { ALE, REST, AUTO }

/** Nutzer-Einstellungen (FR-P1/P4/G4/G6). */
data class Settings(
    val pollIntervalMin: Int = 5,
    val target: TargetRange = TargetRange(),
    val unit: GlucoseUnit = GlucoseUnit.MG_DL,
    val dataSource: DataSourceMode = DataSourceMode.ALE,
    val staleThresholdMin: Int = 10,
    val backgroundEnabled: Boolean = false
)

/** Erlaubte Polling-Stufen: 1..15 in 1er-, 15..60 in 5er-Schritten (FR-P1). */
object PollInterval {
    val allowed: List<Int> = (1..15).toList() + (20..60 step 5).toList()
    fun sanitize(value: Int): Int = allowed.minByOrNull { kotlin.math.abs(it - value) } ?: 5
}

/** Persistente Sitzung — In-Memory (M0), verschluesselt (M1). Genau EIN aktiver Account (FR-A7/A8). */
interface SessionStore {
    suspend fun load(): Session?
    suspend fun save(session: Session)
    suspend fun clear()
}

/** Persistenter Verlauf (FR-G5). Genau EIN aktiver Account -> clear() beim Logout. */
interface HistoryStore {
    suspend fun upsertAll(readings: List<GlucoseReading>)
    suspend fun since(sinceEpochMillis: Long): List<GlucoseReading>
    suspend fun clear()
}

/** Persistente Nutzer-Einstellungen (FR-P). */
interface SettingsStore {
    suspend fun load(): Settings
    suspend fun save(settings: Settings)
}

/** UI-naher Zustand des Glukose-Stroms (FR-S1). */
sealed interface GlucoseState {
    data object LoggedOut : GlucoseState
    data object Loading : GlucoseState
    data class Content(val reading: GlucoseReading, val history: List<GlucoseReading> = emptyList()) : GlucoseState
    data class Error(val message: String) : GlucoseState
}

/** Zentrale, quellenunabhaengige Fassade fuer die Praesentation. */
interface GlucoseRepository {
    val state: StateFlow<GlucoseState>
    val settings: StateFlow<Settings>
    val aleAvailable: Boolean
    suspend fun login(email: String, password: String)
    suspend fun logout()
    suspend fun refreshNow()
    fun updateSettings(transform: (Settings) -> Settings)
}
