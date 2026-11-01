package com.hossam.carenote.core.testing.speech

import com.hossam.carenote.core.domain.speech.SpeechTranscriber
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile

/**
 * [SpeechTranscriber] driven by the test: [emit] events while something collects.
 * Keeps the real contract: completes right after a Final or an Error.
 */
class FakeSpeechTranscriber : SpeechTranscriber {
    private val events = MutableSharedFlow<TranscriptionEvent>()

    /** True only while a collector is listening, so tests can check that stopping really stops. */
    var isListening = false
        private set

    suspend fun emit(event: TranscriptionEvent) {
        events.emit(event)
    }

    override fun transcribe(): Flow<TranscriptionEvent> = events
        .transformWhile { event ->
            emit(event)
            event is TranscriptionEvent.Partial
        }.onStart { isListening = true }
        .onCompletion { isListening = false }
}
