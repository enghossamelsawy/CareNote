package com.hossam.carenote.di

import com.hossam.carenote.core.network.di.BaseUrl
import com.hossam.carenote.core.speech.di.DemoSpeechAllowed
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Prod flavor: the real server, and never the demo speech engine. */
@Module
@InstallIn(SingletonComponent::class)
object EnvironmentModule {
    // Placeholder until a real backend exists.
    @Provides
    @BaseUrl
    fun provideBaseUrl(): String = "https://api.carenote.example/"

    @Provides
    @DemoSpeechAllowed
    fun provideDemoSpeechAllowed(): Boolean = false
}
