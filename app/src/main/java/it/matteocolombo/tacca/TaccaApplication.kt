package it.matteocolombo.tacca

import android.app.Application

class TaccaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
