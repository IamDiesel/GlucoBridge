package de.glucobridge.app

import android.app.Application
import de.glucobridge.app.di.AppContainer

class GlucoBridgeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
