package com.hossam.carenote.core.speech

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import com.hossam.carenote.core.common.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Creates the recognizer; separate from [PlatformSpeechTranscriber] so tests can hand in their own. */
internal interface SpeechRecognizerFactory {
    /** Cheap check, without creating a recognizer. */
    fun isAvailable(): Boolean

    /** Null when the device has no speech engine. Call on the main thread. */
    fun create(): SpeechRecognizer?
}

/** Prefers on-device recognition (API 31+): it works offline and the audio never leaves the phone. */
internal class AndroidSpeechRecognizerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
) : SpeechRecognizerFactory {
    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun create(): SpeechRecognizer? = when {
        isOnDeviceRecognitionAvailable() -> {
            logger.i(TAG, "Using on-device speech recognition")
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        }

        SpeechRecognizer.isRecognitionAvailable(context) -> {
            logger.i(TAG, "Using the default speech recognition service")
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        else -> {
            logger.w(TAG, "No speech recognition service on this device")
            null
        }
    }

    private fun isOnDeviceRecognitionAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    private companion object {
        const val TAG = "SpeechRecognizerFactory"
    }
}
