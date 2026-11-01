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
import javax.inject.Qualifier

/**
 * Whether the demo engine may be used when no real one is ready. The app's mock flavor says yes,
 * prod says no: the demo invents vitals, so in prod the user sees "speech unavailable" instead.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DemoSpeechAllowed

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
            demo: DemoSpeechTranscriber,
            @DemoSpeechAllowed demoAllowed: Boolean,
        ): List<SpeechEngine> = listOfNotNull(mlKit, platform, demo.takeIf { demoAllowed })
    }
}
