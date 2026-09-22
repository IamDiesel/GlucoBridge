package de.glucobridge.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Export-Konfiguration. Nicht-sensibles in SharedPrefs, Nightscout-Secret verschluesselt. */
data class ExportSettings(
    val rangeHours: Int = 24,
    val healthConnectEnabled: Boolean = false,
    val nightscoutEnabled: Boolean = false,
    val nightscoutUrl: String = ""
)

class ExportSettingsStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("glucobridge_export", Context.MODE_PRIVATE)

    private val secure by lazy {
        val key = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            app, "glucobridge_export_secure", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun load(): ExportSettings {
        val d = ExportSettings()
        return ExportSettings(
            rangeHours = prefs.getInt(K_RANGE, d.rangeHours),
            healthConnectEnabled = prefs.getBoolean(K_HC, d.healthConnectEnabled),
            nightscoutEnabled = prefs.getBoolean(K_NS, d.nightscoutEnabled),
            nightscoutUrl = prefs.getString(K_NS_URL, d.nightscoutUrl) ?: d.nightscoutUrl
        )
    }

    fun save(s: ExportSettings) {
        prefs.edit()
            .putInt(K_RANGE, s.rangeHours)
            .putBoolean(K_HC, s.healthConnectEnabled)
            .putBoolean(K_NS, s.nightscoutEnabled)
            .putString(K_NS_URL, s.nightscoutUrl)
            .apply()
    }

    var nightscoutSecret: String
        get() = secure.getString(K_NS_SECRET, "") ?: ""
        set(v) { secure.edit().putString(K_NS_SECRET, v).apply() }

    private companion object {
        const val K_RANGE = "range_hours"
        const val K_HC = "hc_enabled"
        const val K_NS = "ns_enabled"
        const val K_NS_URL = "ns_url"
        const val K_NS_SECRET = "ns_secret"
    }
}
