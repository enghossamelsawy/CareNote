package com.hossam.carenote.core.speech

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.hossam.carenote.core.common.coroutines.CareNoteDispatchers
import com.hossam.carenote.core.common.coroutines.Dispatcher
import com.hossam.carenote.core.common.logging.Logger
import com.hossam.carenote.core.domain.speech.SpeechTranscriber
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/** Android's [SpeechRecognizer] as a [SpeechTranscriber]. Runs on the main thread, as the recognizer requires. */
internal class PlatformSpeechTranscriber @Inject constructor(
    private val recognizerFactory: SpeechRecognizerFactory,
    private val logger: Logger,
    @Dispatcher(CareNoteDispatchers.Main) private val mainDispatcher: CoroutineDispatcher,
) : SpeechTranscriber {
    override fun transcribe(): Flow<TranscriptionEvent> = callbackFlow {
        val recognizer = recognizerFactory.create()
        if (recognizer == null) {
            trySend(TranscriptionEvent.Error(TranscriptionError.Unavailable))
            close()
        } else {
            recognizer.setRecognitionListener(listenerFor(this))
            recognizer.startListening(recognizerIntent())
        }
        // Stops the microphone when the collector cancels, and frees the recognizer either way.
        awaitClose {
            recognizer?.cancel()
            recognizer?.destroy()
        }
    }.flowOn(mainDispatcher)

    private fun listenerFor(events: ProducerScope<TranscriptionEvent>) = object : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults.bestText() ?: return
            events.trySend(TranscriptionEvent.Partial(text))
        }

        override fun onResults(results: Bundle?) {
            val text = results.bestText()
            events.trySend(
                if (text == null) {
                    TranscriptionEvent.Error(TranscriptionError.NoSpeech)
                } else {
                    TranscriptionEvent.Final(text)
                },
            )
            events.close()
        }

        override fun onError(error: Int) {
            // Only the code: the transcript is health data and must not reach logcat.
            logger.w(TAG, "Speech recognition failed with code $error")
            events.trySend(TranscriptionEvent.Error(transcriptionErrorOf(error)))
            events.close()
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun Bundle?.bestText(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    // English, because the vitals parser understands English only.
    private fun recognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)

    private companion object {
        const val TAG = "PlatformSpeechTranscriber"
    }
}
