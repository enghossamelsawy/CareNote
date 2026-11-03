package com.hossam.carenote.core.speech.di

import com.hossam.carenote.core.domain.speech.SpeechTranscriber
import com.hossam.carenote.core.speech.ActiveSpeechEngine
import com.hossam.carenote.core.speech.AndroidSpeechRecognizerFactory
import com.hossam.carenote.core.speech.ConnectivityUnmeteredNetwork
import com.hossam.carenote.core.speech.DemoSpeechTranscriber
import com.hossam.carenote.core.speech.MlKitSpeechTranscriber
import com.hossam.carenote.core.speech.PlatformSpeechTranscriber
import com.hossam.carenote.core.speech.SelectingSpeechTranscriber
import com.hossam.carenote.core.speech.SpeechEngine
import com.hossam.carenote.core.speech.SpeechModel
import com.hossam.carenote.core.speech.SpeechRecognizerFactory
import com.hossam.carenote.core.speech.UnmeteredNetwork
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

    /** The same singleton as [bindSpeechTranscriber], seen only as the name of its current engine. */
    @Binds
    abstract fun bindActiveSpeechEngine(impl: SelectingSpeechTranscriber): ActiveSpeechEngine

    @Binds
    abstract fun bindSpeechRecognizerFactory(impl: AndroidSpeechRecognizerFactory): SpeechRecognizerFactory

    /** The model downloaded at app start; ML Kit's is the only one that needs downloading. */
    @Binds
    abstract fun bindSpeechModel(impl: MlKitSpeechTranscriber): SpeechModel

    @Binds
    abstract fun bindUnmeteredNetwork(impl: ConnectivityUnmeteredNetwork): UnmeteredNetwork

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
