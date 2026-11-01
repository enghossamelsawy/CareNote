package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.speech.SpeechTranscriber
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Listens while collected; cancel the collection to stop. See [SpeechTranscriber.transcribe] for the event order. */
class TranscribeSpeechUseCase @Inject constructor(private val speechTranscriber: SpeechTranscriber) {
    operator fun invoke(): Flow<TranscriptionEvent> = speechTranscriber.transcribe()
}
