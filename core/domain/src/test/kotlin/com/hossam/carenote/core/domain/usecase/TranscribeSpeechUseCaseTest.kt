package com.hossam.carenote.core.domain.usecase

import app.cash.turbine.test
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import com.hossam.carenote.core.testing.speech.FakeSpeechTranscriber
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscribeSpeechUseCaseTest {
    private val speechTranscriber = FakeSpeechTranscriber()
    private val transcribeSpeech = TranscribeSpeechUseCase(speechTranscriber)

    @Test
    fun `given listening, when the engine reports events, then they arrive in order and end at final`() = runTest {
        transcribeSpeech().test {
            // Given
            assertTrue(speechTranscriber.isListening)

            // When
            speechTranscriber.emit(TranscriptionEvent.Partial("Pulse"))
            speechTranscriber.emit(TranscriptionEvent.Final("Pulse 72"))

            // Then
            assertEquals(TranscriptionEvent.Partial("Pulse"), awaitItem())
            assertEquals(TranscriptionEvent.Final("Pulse 72"), awaitItem())
            awaitComplete()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given listening, when the collection is cancelled, then listening stops`() = runTest {
        // Given
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) { transcribeSpeech().collect {} }
        assertTrue(speechTranscriber.isListening)

        // When
        collection.cancelAndJoin()

        // Then
        assertFalse(speechTranscriber.isListening)
    }
}
