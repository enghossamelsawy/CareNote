package com.hossam.carenote.core.speech.di

import com.hossam.carenote.core.domain.speech.SpeechTranscriber
import com.hossam.carenote.core.speech.AndroidSpeechRecognizerFactory
import com.hossam.carenote.core.speech.PlatformSpeechTranscriber
import com.hossam.carenote.core.speech.SpeechRecognizerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SpeechModule {
    // TODO(3.3): bind the provider that picks ML Kit, then this, then the demo fallback.
    @Binds
    fun bindSpeechTranscriber(impl: PlatformSpeechTranscriber): SpeechTranscriber

    @Binds
    fun bindSpeechRecognizerFactory(impl: AndroidSpeechRecognizerFactory): SpeechRecognizerFactory
}
