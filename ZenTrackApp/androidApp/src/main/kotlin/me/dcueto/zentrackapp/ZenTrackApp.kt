package me.dcueto.zentrackapp

import android.app.Application

class ZenTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: startKoin { androidContext(this@ZenTrackApp); modules(...) }
    }
}
