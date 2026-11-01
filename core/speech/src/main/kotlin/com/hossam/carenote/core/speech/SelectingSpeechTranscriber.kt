package com.hossam.carenote.core.speech

import com.hossam.carenote.core.common.logging.Logger
import com.hossam.carenote.core.domain.speech.SpeechTranscriber
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's [SpeechTranscriber]: each recording uses the first engine in [engines] that is ready.
 * Asking again every time means a finished ML Kit download is picked up without restarting the app.
 */
@Singleton
@JvmSuppressWildcards
internal class SelectingSpeechTranscriber @Inject constructor(
    private val engines: List<SpeechEngine>,
    private val logger: Logger,
) : SpeechTranscriber {
    private val _activeEngine = MutableStateFlow<String?>(null)

    /** Name of the engine the last recording used; null before the first one or when none was ready. */
    val activeEngine: StateFlow<String?> = _activeEngine.asStateFlow()

    override fun transcribe(): Flow<TranscriptionEvent> = flow {
        val engine = engines.firstOrNull { it.isReady() }
        _activeEngine.value = engine?.name
        if (engine == null) {
            logger.w(TAG, "No speech engine is ready")
            emit(TranscriptionEvent.Error(TranscriptionError.Unavailable))
        } else {
            logger.i(TAG, "Transcribing with ${engine.name}")
            emitAll(engine.transcribe())
        }
    }

    private companion object {
        const val TAG = "SelectingSpeechTranscriber"
    }
}
