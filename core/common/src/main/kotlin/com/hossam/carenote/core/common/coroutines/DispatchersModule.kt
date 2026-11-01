package com.hossam.carenote.core.common.coroutines

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal object DispatchersModule {
    @Provides
    @Dispatcher(CareNoteDispatchers.IO)
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(CareNoteDispatchers.Default)
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /** Immediate: no extra dispatch when the caller is already on the main thread. */
    @Provides
    @Dispatcher(CareNoteDispatchers.Main)
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}
