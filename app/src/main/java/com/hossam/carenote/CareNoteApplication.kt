package com.hossam.carenote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class CareNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Release builds plant no tree, so they log nothing.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
