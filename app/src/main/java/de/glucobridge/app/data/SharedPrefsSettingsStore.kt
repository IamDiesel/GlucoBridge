package de.glucobridge.app.data

import android.content.Context
import de.glucobridge.core.model.GlucoseUnit
import de.glucobridge.core.model.TargetRange
import de.glucobridge.domain.DataSourceMode
import de.glucobridge.domain.Settings
import de.glucobridge.domain.SettingsStore

/** Einfache, nicht-sensible Einstellungen in SharedPreferences (FR-P). */
class SharedPrefsSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("glucobridge_settings", Context.MODE_PRIVATE)

    override suspend fun load(): Settings {
        val d = Settings()
        return Settings(
            pollIntervalMin = prefs.getInt(K_POLL, d.pollIntervalMin),
            target = TargetRange(
                lowMgDl = prefs.getInt(K_LOW, d.target.lowMgDl),
                highMgDl = prefs.getInt(K_HIGH, d.target.highMgDl)
            ),
            unit = runCatching { GlucoseUnit.valueOf(prefs.getString(K_UNIT, d.unit.name)!!) }.getOrDefault(d.unit),
            dataSource = runCatching { DataSourceMode.valueOf(prefs.getString(K_SRC, d.dataSource.name)!!) }.getOrDefault(d.dataSource),
            staleThresholdMin = prefs.getInt(K_STALE, d.staleThresholdMin),
            backgroundEnabled = prefs.getBoolean(K_BG, d.backgroundEnabled)
        )
    }

    override suspend fun save(settings: Settings) {
        prefs.edit()
            .putInt(K_POLL, settings.pollIntervalMin)
            .putInt(K_LOW, settings.target.lowMgDl)
            .putInt(K_HIGH, settings.target.highMgDl)
            .putString(K_UNIT, settings.unit.name)
            .putString(K_SRC, settings.dataSource.name)
            .putInt(K_STALE, settings.staleThresholdMin)
            .putBoolean(K_BG, settings.backgroundEnabled)
            .apply()
    }

    private companion object {
        const val K_POLL = "poll_interval_min"
        const val K_LOW = "target_low"
        const val K_HIGH = "target_high"
        const val K_UNIT = "unit"
        const val K_SRC = "data_source"
        const val K_STALE = "stale_min"
        const val K_BG = "background_enabled"
    }
}
