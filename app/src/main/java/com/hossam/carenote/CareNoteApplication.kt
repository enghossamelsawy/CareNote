package com.hossam.carenote

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hossam.carenote.core.data.sync.SyncStartup
import com.hossam.carenote.core.speech.SpeechStartup
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Also configures WorkManager, so workers can get their dependencies from Hilt. The default WorkManager
 * initializer is removed in the manifest; WorkManager starts on first use with this configuration instead.
 */
@HiltAndroidApp
class CareNoteApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var speechStartup: SpeechStartup

    @Inject
    lateinit var syncStartup: SyncStartup

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        // Hilt injects the fields above inside super.onCreate().
        super.onCreate()
        // Release builds plant no tree, so they log nothing.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        speechStartup.start()
        syncStartup.start()
    }
}
