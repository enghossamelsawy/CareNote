package com.hossam.carenote.core.network.di

import com.hossam.carenote.core.common.logging.Logger
import com.hossam.carenote.core.network.BuildConfig
import com.hossam.carenote.core.network.CareNoteApi
import com.hossam.carenote.core.network.RemoteDataSource
import com.hossam.carenote.core.network.RetrofitRemoteDataSource
import com.hossam.carenote.core.network.careNoteApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {
    @Multibinds
    @ExtraInterceptors
    abstract fun extraInterceptors(): Set<Interceptor>

    @Binds
    abstract fun bindRemoteDataSource(impl: RetrofitRemoteDataSource): RemoteDataSource

    companion object {
        @Provides
        @Singleton
        @JvmSuppressWildcards
        fun provideOkHttpClient(@ExtraInterceptors extraInterceptors: Set<Interceptor>, logger: Logger): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .apply {
                    // First, so it also logs what the mock server answers. Debug only, and never bodies:
                    // they hold transcripts and vitals.
                    if (BuildConfig.DEBUG) {
                        addInterceptor(
                            HttpLoggingInterceptor { message -> logger.d(HTTP_TAG, message) }
                                .apply { level = HttpLoggingInterceptor.Level.BASIC },
                        )
                    }
                    extraInterceptors.forEach { addInterceptor(it) }
                }.build()

        @Provides
        @Singleton
        fun provideCareNoteApi(client: OkHttpClient): CareNoteApi =
            careNoteApi(baseUrl = BuildConfig.BASE_URL.toHttpUrl(), client = client)

        private const val TIMEOUT_SECONDS = 20L
        private const val HTTP_TAG = "CareNoteHttp"
    }
}
