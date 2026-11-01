package com.hossam.carenote.core.speech

import android.annotation.SuppressLint
import android.speech.SpeechRecognizer
import com.hossam.carenote.core.domain.speech.TranscriptionError

/**
 * Maps a [SpeechRecognizer] error code to the domain's [TranscriptionError].
 * The API 31 codes are plain ints inlined at compile time, so this is safe on older devices too.
 */
@SuppressLint("InlinedApi")
internal fun transcriptionErrorOf(errorCode: Int): TranscriptionError = when (errorCode) {
    SpeechRecognizer.ERROR_NO_MATCH,
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
    -> TranscriptionError.NoSpeech

    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> TranscriptionError.NoPermission

    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> TranscriptionError.Busy

    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
    -> TranscriptionError.Unavailable

    else -> TranscriptionError.Unknown
}
