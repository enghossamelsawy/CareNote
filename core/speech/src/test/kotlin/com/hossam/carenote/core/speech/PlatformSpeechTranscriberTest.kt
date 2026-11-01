package com.hossam.carenote.core.speech

import android.content.Context
import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.hossam.carenote.core.common.logging.Logger
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PlatformSpeechTranscriberTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var recognizer: SpeechRecognizer

    private val androidFactory = SpeechRecognizerFactory {
        SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
    }

    @Test
    fun `given listening, when the engine reports partial then final results, then they arrive in order`() = runTest {
        transcriber(androidFactory).transcribe().test {
            // When
            shadowOf(recognizer).triggerOnPartialResults(results("Pulse"))
            shadowOf(recognizer).triggerOnResults(results("Pulse 72"))

            // Then
            assertEquals(TranscriptionEvent.Partial("Pulse"), awaitItem())
            assertEquals(TranscriptionEvent.Final("Pulse 72"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given listening, when the engine reports an error, then it is mapped and listening ends`() = runTest {
        transcriber(androidFactory).transcribe().test {
            // When
            shadowOf(recognizer).triggerOnError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)

            // Then
            assertEquals(TranscriptionEvent.Error(TranscriptionError.Busy), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given listening, when the final result is empty, then it is no speech`() = runTest {
        transcriber(androidFactory).transcribe().test {
            // When
            shadowOf(recognizer).triggerOnResults(results(""))

            // Then
            assertEquals(TranscriptionEvent.Error(TranscriptionError.NoSpeech), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given no speech engine, when transcribing, then it is unavailable`() = runTest {
        // Given
        val noEngine = SpeechRecognizerFactory { null }

        // When
        transcriber(noEngine).transcribe().test {
            // Then
            assertEquals(TranscriptionEvent.Error(TranscriptionError.Unavailable), awaitItem())
            awaitComplete()
        }
    }

    // Unconfined: the recognizer is created as soon as collection starts, like on the real main thread.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.transcriber(factory: SpeechRecognizerFactory) =
        PlatformSpeechTranscriber(factory, NoOpLogger, UnconfinedTestDispatcher(testScheduler))

    private fun results(text: String) = Bundle().apply {
        putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text))
    }

    private object NoOpLogger : Logger {
        override fun d(tag: String, message: String) = Unit

        override fun i(tag: String, message: String) = Unit

        override fun w(tag: String, message: String, throwable: Throwable?) = Unit

        override fun e(tag: String, message: String, throwable: Throwable?) = Unit
    }
}
