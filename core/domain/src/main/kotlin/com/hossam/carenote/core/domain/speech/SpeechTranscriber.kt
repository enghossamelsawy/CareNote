package com.hossam.carenote.core.domain.speech

import kotlinx.coroutines.flow.Flow

/** Turns the user's speech into text. Implemented outside the domain (ML Kit, the platform recognizer, a demo). */
interface SpeechTranscriber {
    /**
     * Cold: collecting starts listening, cancelling stops and releases the recognizer.
     * Emits any number of [TranscriptionEvent.Partial], then exactly one [TranscriptionEvent.Final]
     * or [TranscriptionEvent.Error], then completes.
     */
    fun transcribe(): Flow<TranscriptionEvent>
}
