package com.hossam.carenote.di

import com.hossam.carenote.core.network.di.BaseUrl
import com.hossam.carenote.core.speech.di.DemoSpeechAllowed
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Mock flavor: the mock server answers every request, so the address is never resolved. */
@Module
@InstallIn(SingletonComponent::class)
object EnvironmentModule {
    @Provides
    @BaseUrl
    fun provideBaseUrl(): String = "https://mock.carenote.invalid/"

    @Provides
    @DemoSpeechAllowed
    fun provideDemoSpeechAllowed(): Boolean = true
}
