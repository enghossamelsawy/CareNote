package com.hossam.carenote.core.speech

import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Pretends to hear a fixed sentence, word by word, so the app can be demoed on an emulator without a speech engine.
 * It invents vitals, so it must never reach real users: TODO(4.3) limit it to the mock flavor.
 */
internal class DemoSpeechTranscriber @Inject constructor() : SpeechEngine {
    override val name = "Demo"

    override suspend fun isReady() = true

    override fun transcribe(): Flow<TranscriptionEvent> = flow {
        val words = SAMPLE_NOTE.split(" ")
        for (count in 1..words.size) {
            delay(WORD_DELAY)
            emit(TranscriptionEvent.Partial(words.take(count).joinToString(" ")))
        }
        emit(TranscriptionEvent.Final(SAMPLE_NOTE))
    }

    companion object {
        /** Uses each vital the parser knows, so the demo fills every field. */
        const val SAMPLE_NOTE = "Blood pressure 120 over 80, pulse 72, temperature 36.8, SpO2 97"

        /** Roughly the pace of someone dictating. */
        val WORD_DELAY = 300.milliseconds
    }
}
