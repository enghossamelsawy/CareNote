package com.hossam.carenote.core.domain.speech

/** What a [SpeechTranscriber] reports while listening. Errors are values, so callers never need try/catch. */
sealed interface TranscriptionEvent {
    /** The best guess so far; replaced by the next event. */
    data class Partial(val text: String) : TranscriptionEvent

    data class Final(val text: String) : TranscriptionEvent

    data class Error(val reason: TranscriptionError) : TranscriptionEvent
}

enum class TranscriptionError {
    /** Nothing was said, or nothing could be recognised. */
    NoSpeech,

    /** The microphone permission is missing. */
    NoPermission,

    /** The recognizer is already in use. */
    Busy,

    /** No speech engine is available on this device. */
    Unavailable,

    Unknown,
}
