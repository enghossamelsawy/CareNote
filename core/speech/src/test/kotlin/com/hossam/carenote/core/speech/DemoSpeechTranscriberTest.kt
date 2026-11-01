package com.hossam.carenote.core.speech

import app.cash.turbine.test
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoSpeechTranscriberTest {
    private val transcriber = DemoSpeechTranscriber()

    @Test
    fun `given the demo engine, when transcribing, then words arrive one by one and end with the note`() = runTest {
        // Given
        val words = DemoSpeechTranscriber.SAMPLE_NOTE.split(" ")

        // When
        transcriber.transcribe().test {
            // Then
            assertEquals(TranscriptionEvent.Partial(words.first()), awaitItem())
            assertEquals(TranscriptionEvent.Partial(words.take(2).joinToString(" ")), awaitItem())
            repeat(words.size - 2) { awaitItem() }
            assertEquals(TranscriptionEvent.Final(DemoSpeechTranscriber.SAMPLE_NOTE), awaitItem())
            awaitComplete()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given the demo is talking, when the collection is cancelled, then no more words arrive`() = runTest {
        // Given
        val received = mutableListOf<TranscriptionEvent>()
        val collection = launch { transcriber.transcribe().collect { received += it } }
        advanceTimeBy(DemoSpeechTranscriber.WORD_DELAY * 2)
        runCurrent()

        // When
        collection.cancel()
        advanceTimeBy(DemoSpeechTranscriber.WORD_DELAY * 10)

        // Then
        assertEquals(2, received.size)
        assertTrue(received.none { it is TranscriptionEvent.Final })
    }

    @Test
    fun `given the demo engine, when asked if ready, then it always is`() = runTest {
        // When
        val ready = transcriber.isReady()

        // Then
        assertTrue(ready)
    }
}
