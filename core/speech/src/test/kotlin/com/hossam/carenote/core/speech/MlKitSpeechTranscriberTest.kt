package com.hossam.carenote.core.speech

import app.cash.turbine.test
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** On phones older than Android 12 the engine must step aside without touching ML Kit. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class MlKitSpeechTranscriberTest {
    private val transcriber = MlKitSpeechTranscriber(NoOpLogger)

    @Test
    fun `given Android 8, when asked if it is ready, then it is not`() = runTest {
        // When
        val ready = transcriber.isReady()

        // Then
        assertFalse(ready)
    }

    @Test
    fun `given Android 8, when transcribing anyway, then it reports unavailable instead of crashing`() = runTest {
        // When
        transcriber.transcribe().test {
            // Then
            assertEquals(TranscriptionEvent.Error(TranscriptionError.Unavailable), awaitItem())
            awaitComplete()
        }
    }
}
