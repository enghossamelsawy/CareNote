package com.hossam.carenote.core.speech

import android.speech.SpeechRecognizer
import com.hossam.carenote.core.domain.speech.TranscriptionError
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechErrorMappingTest {
    @Test
    fun `given nothing was recognised, when mapping, then it is no speech`() {
        // Given
        val errorCodes = listOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT)

        // When
        val errors = errorCodes.map(::transcriptionErrorOf)

        // Then
        assertEquals(listOf(TranscriptionError.NoSpeech, TranscriptionError.NoSpeech), errors)
    }

    @Test
    fun `given a missing permission, when mapping, then it is no permission`() {
        // Given
        val errorCode = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS

        // When
        val error = transcriptionErrorOf(errorCode)

        // Then
        assertEquals(TranscriptionError.NoPermission, error)
    }

    @Test
    fun `given a busy recognizer, when mapping, then it is busy`() {
        // Given
        val errorCode = SpeechRecognizer.ERROR_RECOGNIZER_BUSY

        // When
        val error = transcriptionErrorOf(errorCode)

        // Then
        assertEquals(TranscriptionError.Busy, error)
    }

    @Test
    fun `given the engine or language is missing, when mapping, then it is unavailable`() {
        // Given
        val errorCodes = listOf(
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
        )

        // When
        val errors = errorCodes.map(::transcriptionErrorOf)

        // Then
        assertEquals(List(3) { TranscriptionError.Unavailable }, errors)
    }

    @Test
    fun `given any other error, when mapping, then it is unknown`() {
        // Given
        val errorCodes = listOf(SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_NETWORK, -1)

        // When
        val errors = errorCodes.map(::transcriptionErrorOf)

        // Then
        assertEquals(List(3) { TranscriptionError.Unknown }, errors)
    }
}
