package de.glucobridge.app.di

import android.content.Context
import androidx.room.Room
import de.glucobridge.app.data.EncryptedSessionStore
import de.glucobridge.app.data.GlucoBridgeDatabase
import de.glucobridge.app.data.RoomHistoryStore
import de.glucobridge.app.data.SharedPrefsSettingsStore
import de.glucobridge.data.repository.GlucoseRepositoryImpl
import de.glucobridge.data.repository.SourceSelector
import de.glucobridge.data.rest.LibreLinkUpRestSource
import de.glucobridge.app.wear.WearSyncer
import de.glucobridge.domain.GlucoseRepository

/** Manuelles Constructor-DI (schlank). Einziger Ort, an dem konkrete Klassen verdrahtet werden. */
class AppContainer(context: Context) {
    private val restSource = LibreLinkUpRestSource()
    private val aleSource = AleSourceLoader.tryLoad()   // null, wenn IP-Zone fehlt -> REST

    private val selector = SourceSelector(ale = aleSource, rest = restSource)
    private val sessionStore = EncryptedSessionStore(context.applicationContext)
    private val db = Room.databaseBuilder(
        context.applicationContext,
        GlucoBridgeDatabase::class.java,
        "glucobridge.db"
    ).build()
    private val historyStore = RoomHistoryStore(db.glucoseDao())
    private val settingsStore = SharedPrefsSettingsStore(context.applicationContext)

    val glucoseRepository: GlucoseRepository = GlucoseRepositoryImpl(selector, sessionStore, historyStore, settingsStore)
    @Suppress("unused")
    private val wearSyncer = WearSyncer(context.applicationContext, glucoseRepository)

    val aleAvailable: Boolean get() = selector.aleAvailable
}
