package com.hossam.carenote.core.speech

import com.hossam.carenote.core.domain.speech.SpeechTranscriber

/** One speech engine the app can choose from at runtime. Only `core:speech` knows engines exist. */
internal interface SpeechEngine : SpeechTranscriber {
    /** For logs and the debug menu. */
    val name: String

    /** True when [transcribe] can start right away, without a download or a missing service. Never throws. */
    suspend fun isReady(): Boolean
}
