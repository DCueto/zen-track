package me.dcueto.zentrackapp

import android.app.Application
import me.dcueto.zentrackapp.di.androidAppModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ZenTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ZenTrackApp)
            modules(androidAppModule)
        }
    }
}
