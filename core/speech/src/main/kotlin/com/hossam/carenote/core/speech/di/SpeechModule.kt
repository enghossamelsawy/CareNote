package com.hossam.carenote.core.speech.di

import com.hossam.carenote.core.domain.speech.SpeechTranscriber
import com.hossam.carenote.core.speech.AndroidSpeechRecognizerFactory
import com.hossam.carenote.core.speech.DemoSpeechTranscriber
import com.hossam.carenote.core.speech.MlKitSpeechTranscriber
import com.hossam.carenote.core.speech.PlatformSpeechTranscriber
import com.hossam.carenote.core.speech.SelectingSpeechTranscriber
import com.hossam.carenote.core.speech.SpeechEngine
import com.hossam.carenote.core.speech.SpeechRecognizerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SpeechModule {
    @Binds
    abstract fun bindSpeechTranscriber(impl: SelectingSpeechTranscriber): SpeechTranscriber

    @Binds
    abstract fun bindSpeechRecognizerFactory(impl: AndroidSpeechRecognizerFactory): SpeechRecognizerFactory

    companion object {
        /** Preference order. Adding an engine or changing the order only touches this list. */
        @Provides
        fun provideSpeechEngines(
            mlKit: MlKitSpeechTranscriber,
            platform: PlatformSpeechTranscriber,
            // TODO(4.3): mock flavor only; prod ends with "unavailable" instead of invented vitals.
            demo: DemoSpeechTranscriber,
        ): List<SpeechEngine> = listOf(mlKit, platform, demo)
    }
}
