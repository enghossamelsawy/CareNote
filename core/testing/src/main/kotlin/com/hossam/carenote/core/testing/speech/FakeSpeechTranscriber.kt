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

    /** How many collectors are listening right now; more than one means two recordings hold the microphone. */
    var listenerCount = 0
        private set

    /** True only while a collector is listening, so tests can check that stopping really stops. */
    val isListening: Boolean
        get() = listenerCount > 0

    suspend fun emit(event: TranscriptionEvent) {
        events.emit(event)
    }

    override fun transcribe(): Flow<TranscriptionEvent> = events
        .transformWhile { event ->
            emit(event)
            event is TranscriptionEvent.Partial
        }.onStart { listenerCount++ }
        .onCompletion { listenerCount-- }
}
