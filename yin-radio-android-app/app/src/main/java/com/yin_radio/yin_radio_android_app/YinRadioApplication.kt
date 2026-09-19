package com.yin_radio.yin_radio_android_app

import android.app.Application
import android.util.Log
import com.yin_radio.yin_radio_android_app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class YinRadioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.v("YinRadioApplication", "onCreate()")
        startKoin {
            // Log Koin into Android logger
            androidLogger();

            // Reference Android context
            androidContext(this@YinRadioApplication)

            // Load modules
            modules(appModule)
        }
    }
}
