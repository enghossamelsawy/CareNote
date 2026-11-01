package com.hossam.carenote

import android.app.Application
import com.hossam.carenote.core.speech.SpeechStartup
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CareNoteApplication : Application() {
    @Inject
    lateinit var speechStartup: SpeechStartup

    override fun onCreate() {
        // Hilt injects the fields above inside super.onCreate().
        super.onCreate()
        // Release builds plant no tree, so they log nothing.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        speechStartup.start()
    }
}
