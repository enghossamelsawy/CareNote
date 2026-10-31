package com.hossam.carenote.core.data.di

import com.hossam.carenote.core.data.repository.OfflineFirstCareNoteRepository
import com.hossam.carenote.core.data.repository.OfflineFirstResidentRepository
import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.ResidentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindResidentRepository(impl: OfflineFirstResidentRepository): ResidentRepository

    @Binds
    abstract fun bindCareNoteRepository(impl: OfflineFirstCareNoteRepository): CareNoteRepository

    companion object {
        /** Injected instead of calling Instant.now(), so tests can use a fixed time. */
        @Provides
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
