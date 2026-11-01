package com.hossam.carenote.core.speech

import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SelectingSpeechTranscriberTest {
    private val mlKit = FakeEngine(name = "ML Kit")
    private val android = FakeEngine(name = "Android")
    private val demo = FakeEngine(name = "Demo")
    private val transcriber = SelectingSpeechTranscriber(listOf(mlKit, android, demo), NoOpLogger)

    @Test
    fun `given the first engine is not ready, when transcribing, then the next ready one is used`() = runTest {
        // Given
        mlKit.ready = false

        // When
        val events = transcriber.transcribe().toList()

        // Then
        assertEquals(listOf(TranscriptionEvent.Final("heard by Android")), events)
        assertFalse(mlKit.started)
    }

    @Test
    fun `given several ready engines, when transcribing, then only the first one starts`() = runTest {
        // When
        transcriber.transcribe().toList()

        // Then
        assertEquals(listOf(true, false, false), listOf(mlKit.started, android.started, demo.started))
    }

    @Test
    fun `given no engine is ready, when transcribing, then speech is unavailable`() = runTest {
        // Given
        listOf(mlKit, android, demo).forEach { it.ready = false }

        // When
        val events = transcriber.transcribe().toList()

        // Then
        assertEquals(listOf(TranscriptionEvent.Error(TranscriptionError.Unavailable)), events)
        assertNull(transcriber.activeEngine.value)
    }

    @Test
    fun `given a recording, when an engine is chosen, then it becomes the active engine`() = runTest {
        // Given
        mlKit.ready = false

        // When
        transcriber.transcribe().toList()

        // Then
        assertEquals("Android", transcriber.activeEngine.value)
    }

    private class FakeEngine(override val name: String) : SpeechEngine {
        var ready = true
        var started = false
            private set

        override suspend fun isReady() = ready

        override fun transcribe(): Flow<TranscriptionEvent> = flow {
            started = true
            emit(TranscriptionEvent.Final("heard by $name"))
        }
    }
}
