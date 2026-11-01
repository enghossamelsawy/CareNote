package com.hossam.carenote.core.speech

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import com.hossam.carenote.core.common.logging.Logger
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

/**
 * ML Kit GenAI speech recognition in Basic mode: the on-device model, API 31+.
 * The library is alpha, so every call is guarded; a failure makes the engine "not ready" instead of crashing.
 *
 * ML Kit may keep listening across pauses and report each finished sentence as its own final segment.
 * Segments are joined and sent as a growing [TranscriptionEvent.Partial]; the single
 * [TranscriptionEvent.Final] comes when ML Kit completes. Stopping early means cancelling the collection.
 */
internal class MlKitSpeechTranscriber @Inject constructor(private val logger: Logger) : SpeechEngine {
    override val name = "ML Kit"

    override suspend fun isReady(): Boolean = guarded(default = false) { status() == FeatureStatus.AVAILABLE }

    /** Downloads the model if the device can have it. Slow; call it in the background, not when recording. */
    suspend fun prepare() {
        guarded(default = Unit) {
            if (status() != FeatureStatus.DOWNLOADABLE) return@guarded
            logger.i(TAG, "Downloading the ML Kit speech model")
            SpeechRecognition.getClient(options()).use { client ->
                client.download().collect { downloadStatus ->
                    when (downloadStatus) {
                        is DownloadStatus.DownloadCompleted -> logger.i(TAG, "ML Kit speech model downloaded")
                        is DownloadStatus.DownloadFailed -> logger.w(TAG, "ML Kit speech model download failed")
                        else -> Unit
                    }
                }
            }
        }
    }

    /** Below Android 12 there is no model to listen with, so it reports "unavailable" instead of crashing. */
    override fun transcribe(): Flow<TranscriptionEvent> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        recognize()
    } else {
        flowOf(TranscriptionEvent.Error(TranscriptionError.Unavailable))
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun recognize(): Flow<TranscriptionEvent> = flow {
        SpeechRecognition.getClient(options()).use { client ->
            try {
                emitAll(client.startRecognition(micRequest()).asTranscriptionEvents())
            } finally {
                // Releases the microphone on cancel too; it must run even though we're being cancelled.
                withContext(NonCancellable) { runCatching { client.stopRecognition() } }
            }
        }
    }.catch { error ->
        logger.w(TAG, "ML Kit recognition failed", error)
        emit(TranscriptionEvent.Error(TranscriptionError.Unavailable))
    }

    private fun Flow<SpeechRecognizerResponse>.asTranscriptionEvents(): Flow<TranscriptionEvent> {
        val finishedSegments = mutableListOf<String>()
        fun textWith(current: String) = (finishedSegments + current).filter { it.isNotBlank() }.joinToString(" ")

        return transformWhile { response ->
            when (response) {
                is SpeechRecognizerResponse.PartialTextResponse -> {
                    if (response.text.isNotBlank()) emit(TranscriptionEvent.Partial(textWith(response.text)))
                    true
                }

                is SpeechRecognizerResponse.FinalTextResponse -> {
                    finishedSegments += response.text
                    emit(TranscriptionEvent.Partial(textWith("")))
                    true
                }

                is SpeechRecognizerResponse.ErrorResponse -> {
                    logger.w(TAG, "ML Kit reported a recognition error")
                    emit(TranscriptionEvent.Error(TranscriptionError.Unknown))
                    false
                }

                is SpeechRecognizerResponse.CompletedResponse -> {
                    val text = textWith("")
                    emit(
                        if (text.isEmpty()) {
                            TranscriptionEvent.Error(TranscriptionError.NoSpeech)
                        } else {
                            TranscriptionEvent.Final(text)
                        },
                    )
                    false
                }

                // Audio levels: not used yet.
                else -> true
            }
        }
    }

    private suspend fun status(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return FeatureStatus.UNAVAILABLE
        return SpeechRecognition.getClient(options()).use { it.checkStatus() }
    }

    private suspend fun <T> guarded(default: T, block: suspend () -> T): T = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        logger.w(TAG, "ML Kit is not usable", error)
        default
    }

    // English, because the vitals parser understands English only.
    private fun options(): SpeechRecognizerOptions = speechRecognizerOptions {
        locale = Locale.US
        preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun micRequest() = speechRecognizerRequest { audioSource = AudioSource.fromMic() }

    private companion object {
        const val TAG = "MlKitSpeechTranscriber"
    }
}
