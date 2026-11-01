package com.hossam.carenote.core.network.mock

import com.hossam.carenote.core.network.IDEMPOTENCY_KEY_HEADER
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import kotlin.random.Random

class MockServerInterceptorTest {
    private val server = MockServer()
    private val settings = MockServerSettings().apply { update { copy(latencyMillis = 0L..0L) } }

    // A host that doesn't exist: if a request ever left the interceptor, the test would fail.
    private val client = OkHttpClient.Builder()
        .addInterceptor(MockServerInterceptor(server, settings, Random(seed = 1)))
        .build()

    @Test
    fun `given the mock server, when residents are requested, then it answers without the network`() {
        // When
        val code = client.newCall(get("residents")).execute().use { it.code }

        // Then
        assertEquals(MockServer.OK, code)
    }

    @Test
    fun `given a note, when it is posted, then the server receives its body and key`() {
        // Given
        val body = """{"id":"n-1","residentId":"res-101","transcript":"Pulse 72","vitals":{},"createdAt":"2026"}"""
        val request = Request.Builder()
            .url("$BASE_URL/notes")
            .header(IDEMPOTENCY_KEY_HEADER, "n-1")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        // When
        val code = client.newCall(request).execute().use { it.code }

        // Then
        assertEquals(MockServer.CREATED, code)
        assertEquals(listOf("n-1"), server.storedNotes.map { it.id })
    }

    @Test
    fun `given offline is on, when requesting, then it fails like airplane mode`() {
        // Given
        settings.update { copy(offline = true) }

        // When / Then
        assertThrows(IOException::class.java) { client.newCall(get("residents")).execute() }
    }

    @Test
    fun `given every request fails, when requesting, then the server is unavailable`() {
        // Given
        settings.update { copy(failureRate = 1.0) }

        // When
        val code = client.newCall(get("residents")).execute().use { it.code }

        // Then
        assertEquals(503, code)
    }

    private fun get(path: String) = Request.Builder().url("$BASE_URL/$path").build()

    private companion object {
        const val BASE_URL = "https://mock.carenote.invalid"
    }
}
