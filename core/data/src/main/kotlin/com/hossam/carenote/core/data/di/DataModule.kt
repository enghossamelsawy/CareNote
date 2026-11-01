package com.hossam.carenote.core.data.di

import android.content.Context
import androidx.work.WorkManager
import com.hossam.carenote.core.data.network.ConnectivityManagerNetworkMonitor
import com.hossam.carenote.core.data.repository.OfflineFirstCareNoteRepository
import com.hossam.carenote.core.data.repository.OfflineFirstResidentRepository
import com.hossam.carenote.core.data.sync.WorkManagerSyncScheduler
import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.NetworkMonitor
import com.hossam.carenote.core.domain.repository.ResidentRepository
import com.hossam.carenote.core.domain.repository.SyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindResidentRepository(impl: OfflineFirstResidentRepository): ResidentRepository

    @Binds
    abstract fun bindCareNoteRepository(impl: OfflineFirstCareNoteRepository): CareNoteRepository

    @Binds
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityManagerNetworkMonitor): NetworkMonitor

    companion object {
        /** Injected instead of calling Instant.now(), so tests can use a fixed time. */
        @Provides
        fun provideClock(): Clock = Clock.systemUTC()

        /**
         * Configured with Hilt's worker factory by the app, see CareNoteApplication. Inject it as
         * `Lazy<WorkManager>` in anything the Application itself depends on, or startup crashes.
         */
        @Provides
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
    }
}
