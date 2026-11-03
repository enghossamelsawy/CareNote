package com.hossam.carenote.core.speech

/** An on-device speech model that may have to be downloaded before it can be used. */
internal interface SpeechModel {
    /** True when this phone can have the model but hasn't downloaded it yet. Never throws. */
    suspend fun canDownloadModel(): Boolean

    /** Downloads the model if the device can have it. Slow; call it in the background, not when recording. */
    suspend fun downloadModel()
}
