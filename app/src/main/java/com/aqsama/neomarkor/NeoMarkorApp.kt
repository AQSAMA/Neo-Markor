package com.aqsama.neomarkor

import android.app.Application
import com.aqsama.neomarkor.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NeoMarkorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NeoMarkorApp)
            modules(appModule)
        }
    }
}
