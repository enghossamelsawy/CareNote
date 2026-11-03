package com.hossam.carenote.core.network.di

import com.hossam.carenote.core.network.CareNoteApi
import com.hossam.carenote.core.network.RemoteDataSource
import com.hossam.carenote.core.network.RetrofitRemoteDataSource
import com.hossam.carenote.core.network.careNoteApi
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.Optional
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * OkHttp interceptors other modules add with `@IntoSet`, such as the mock server in the mock flavor.
 * Empty in prod, so this module never needs to know the mock exists.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ExtraInterceptors

/**
 * The interceptor that logs requests. Only debug builds bind one, so this module never needs to know
 * the build type. Not part of [ExtraInterceptors]: a set has no order, and logging must come first.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class LoggingInterceptor

/** The server's address, ending in "/". Each app flavor provides its own. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BaseUrl

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {
    @Multibinds
    @ExtraInterceptors
    abstract fun extraInterceptors(): Set<Interceptor>

    /** Release builds bind nothing here, so they never log requests. */
    @BindsOptionalOf
    @LoggingInterceptor
    abstract fun loggingInterceptor(): Interceptor

    @Binds
    abstract fun bindRemoteDataSource(impl: RetrofitRemoteDataSource): RemoteDataSource

    companion object {
        @Provides
        @Singleton
        @JvmSuppressWildcards
        fun provideOkHttpClient(
            @ExtraInterceptors extraInterceptors: Set<Interceptor>,
            @LoggingInterceptor logging: Optional<Interceptor>,
        ): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // First, so it also logs what the mock server answers.
            .apply { logging.ifPresent(::addInterceptor) }
            .apply { extraInterceptors.forEach(::addInterceptor) }
            .build()

        @Provides
        @Singleton
        fun provideCareNoteApi(@BaseUrl baseUrl: String, client: OkHttpClient): CareNoteApi =
            careNoteApi(baseUrl = baseUrl.toHttpUrl(), client = client)

        private const val TIMEOUT_SECONDS = 20L
    }
}
