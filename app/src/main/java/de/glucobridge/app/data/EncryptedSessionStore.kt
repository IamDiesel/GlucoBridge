package de.glucobridge.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.glucobridge.core.api.Session
import de.glucobridge.domain.SessionStore

/**
 * Verschluesselte, persistente Sitzung (FR-A2/A8). Genau EIN aktiver Account;
 * clear() (Logout) entfernt alle Reste (FR-A4).
 */
class EncryptedSessionStore(context: Context) : SessionStore {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "glucobridge_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun load(): Session? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val account = prefs.getString(KEY_ACCOUNT, null) ?: return null
        return Session(
            accountId = account,
            token = token,
            installationId = prefs.getString(KEY_IID, "").orEmpty(),
            region = prefs.getString(KEY_REGION, null),
            opaque = prefs.getString(KEY_OPAQUE, null)
        )
    }

    override suspend fun save(session: Session) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_ACCOUNT, session.accountId)
            .putString(KEY_IID, session.installationId)
            .putString(KEY_REGION, session.region)
            .putString(KEY_OPAQUE, session.opaque)
            .apply()
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_ACCOUNT = "account"
        const val KEY_IID = "iid"
        const val KEY_REGION = "region"
        const val KEY_OPAQUE = "opaque"
    }
}
