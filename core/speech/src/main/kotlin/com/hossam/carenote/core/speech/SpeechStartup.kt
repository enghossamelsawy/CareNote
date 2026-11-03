package com.hossam.carenote.core.speech

import com.hossam.carenote.core.common.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Called once at app start, so the ML Kit model is downloaded before the first recording needs it. The model is
 * large and a care home's phones may be on limited or paid mobile data, so the download waits for Wi-Fi or
 * another unmetered connection; until then, recording uses Android's own recognizer.
 */
class SpeechStartup @Inject internal constructor(
    private val speechModel: SpeechModel,
    private val unmeteredNetwork: UnmeteredNetwork,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    fun start() {
        applicationScope.launch {
            // Only listens for Wi-Fi when there is something to download.
            if (!speechModel.canDownloadModel()) return@launch
            unmeteredNetwork.await()
            speechModel.downloadModel()
        }
    }
}
