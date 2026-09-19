package de.glucobridge.data.repository

import de.glucobridge.core.api.GlucoseSource
import de.glucobridge.core.api.Session
import de.glucobridge.core.api.SourceException
import de.glucobridge.domain.DataSourceMode
import de.glucobridge.domain.GlucoseRepository
import de.glucobridge.domain.GlucoseState
import de.glucobridge.domain.HistoryStore
import de.glucobridge.domain.PollInterval
import de.glucobridge.domain.SessionStore
import de.glucobridge.domain.SettingsStore
import de.glucobridge.domain.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Waehlt die Datenquelle gemaess Settings (FR-P7): Default ALE, Fallback REST.
 * Build-Praesenz => Verfuegbarkeit, Setting => Praeferenz, hier => Fallback.
 */
class SourceSelector(
    private val ale: GlucoseSource?,   // null, wenn IP-Zone nicht gebaut
    private val rest: GlucoseSource
) {
    val aleAvailable: Boolean get() = ale != null
    val restSource: GlucoseSource get() = rest
    fun select(mode: DataSourceMode): GlucoseSource = when (mode) {
        DataSourceMode.REST -> rest
        DataSourceMode.ALE, DataSourceMode.AUTO -> ale ?: rest
    }
    /** Quelle passend zur bestehenden Session: opaque (kp) gesetzt => ALE, sonst REST. */
    fun forSession(session: Session): GlucoseSource =
        if (session.opaque != null && ale != null) ale else rest
}

class GlucoseRepositoryImpl(
    private val selector: SourceSelector,
    private val sessionStore: SessionStore,
    private val historyStore: HistoryStore,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : GlucoseRepository {

    private val _state = MutableStateFlow<GlucoseState>(GlucoseState.LoggedOut)
    override val state: StateFlow<GlucoseState> = _state.asStateFlow()

    private val _settings = MutableStateFlow(Settings())
    override val settings: StateFlow<Settings> = _settings.asStateFlow()

    override val aleAvailable: Boolean get() = selector.aleAvailable

    private var pollJob: Job? = null

    init {
        scope.launch {
            _settings.value = runCatching { settingsStore.load() }.getOrDefault(Settings())
            if (sessionStore.load() != null) {          // FR-A3: Auto-Restore
                _state.value = GlucoseState.Loading
                startPolling()
            }
        }
    }

    override suspend fun login(email: String, password: String) {
        _state.value = GlucoseState.Loading
        try {
            val preferred = selector.select(_settings.value.dataSource)
            val session = try {
                preferred.login(email, password)
            } catch (t: Throwable) {
                // FR-P7: ALE fehlgeschlagen -> automatischer REST-Fallback (eigener Login)
                if (preferred !== selector.restSource) selector.restSource.login(email, password) else throw t
            }
            sessionStore.save(session)
            startPolling()
        } catch (t: Throwable) {
            _state.value = GlucoseState.Error(t.message ?: "Login fehlgeschlagen")
        }
    }

    override suspend fun logout() {
        pollJob?.cancel(); pollJob = null
        sessionStore.clear()
        runCatching { historyStore.clear() }   // Verlauf ist accountgebunden -> beim Wechsel leeren (FR-A4)
        _state.value = GlucoseState.LoggedOut
    }

    override suspend fun refreshNow() = fetchOnce()

    override fun updateSettings(transform: (Settings) -> Settings) {
        val next = transform(_settings.value).let { it.copy(pollIntervalMin = PollInterval.sanitize(it.pollIntervalMin)) }
        _settings.value = next
        scope.launch { runCatching { settingsStore.save(next) } }
        // Kein sofortiger Poll-Neustart: das neue Intervall greift ab dem naechsten Zyklus
        // (die Poll-Schleife liest pollIntervalMin bei jedem delay neu). Verhindert API-Hammering
        // beim Sliden und dadurch ausgeloeste transiente Fehler-/LoggedOut-Zustaende.
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                fetchOnce()
                delay(_settings.value.pollIntervalMin * 60_000L)
            }
        }
    }

    private suspend fun fetchOnce() {
        val session: Session = sessionStore.load() ?: run {
            _state.value = GlucoseState.LoggedOut; return
        }
        try {
            val source = selector.forSession(session)
            val reading = source.latest(session)
            val fresh = runCatching { source.history(session, 12) }.getOrDefault(emptyList())
            // FR-G5: frische Werte + aktuellen Messwert persistieren, Verlauf aus der DB mergen
            val history = runCatching {
                historyStore.upsertAll(fresh + reading)
                historyStore.since(System.currentTimeMillis() - DISPLAY_WINDOW_MS)
            }.getOrElse { if (fresh.isEmpty()) listOf(reading) else fresh }
            _state.value = GlucoseState.Content(reading, history)
        } catch (t: Throwable) {
            if (t is SourceException.SessionExpired) {
                // Wertlose/abgelaufene Session verwerfen -> sauber zurueck zum Login (FR-A4)
                pollJob?.cancel(); pollJob = null
                sessionStore.clear()
                _state.value = GlucoseState.LoggedOut
            } else {
                _state.value = GlucoseState.Error(t.message ?: "Abruf fehlgeschlagen")
            }
        }
    }

    private companion object {
        const val DISPLAY_WINDOW_MS = 24L * 60 * 60 * 1000   // Anzeige-Fenster fuer den Verlauf
    }
}
