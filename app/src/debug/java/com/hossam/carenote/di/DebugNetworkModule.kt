package com.hossam.carenote.di

import com.hossam.carenote.core.common.logging.Logger
import com.hossam.carenote.core.network.di.LoggingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Debug builds log every request: method, URL, status and time. Never the bodies: they hold transcripts
 * and vitals. Release builds have neither this module nor the logging library.
 */
@Module
@InstallIn(SingletonComponent::class)
object DebugNetworkModule {
    @Provides
    @LoggingInterceptor
    fun provideLoggingInterceptor(logger: Logger): Interceptor =
        HttpLoggingInterceptor { message -> logger.d(HTTP_TAG, message) }
            .apply { level = HttpLoggingInterceptor.Level.BASIC }

    private const val HTTP_TAG = "CareNoteHttp"
}
