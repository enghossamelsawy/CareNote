package com.hossam.carenote.core.speech

import com.hossam.carenote.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Called once at app start, so the ML Kit model is downloaded before the first recording needs it. */
class SpeechStartup @Inject internal constructor(
    private val mlKitSpeechTranscriber: MlKitSpeechTranscriber,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    fun start() {
        applicationScope.launch { mlKitSpeechTranscriber.prepare() }
    }
}
