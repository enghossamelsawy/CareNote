package com.hossam.carenote.core.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DefaultHeadersInterceptorTest {
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `given a plain request, when sending it, then accept, user agent and request id are added`() {
        // Given
        val interceptor = DefaultHeadersInterceptor(ClientInfo("CareNote/1.0 (Android 15; Pixel 8)")) { "request-1" }

        // When
        send(interceptor)

        // Then
        val headers = server.takeRequest().headers
        assertEquals("application/json", headers["Accept"])
        assertEquals("CareNote/1.0 (Android 15; Pixel 8)", headers["User-Agent"])
        assertEquals("request-1", headers["X-Request-Id"])
    }

    @Test
    fun `given two requests, when sending them, then each gets a new request id`() {
        // Given
        val interceptor = DefaultHeadersInterceptor(ClientInfo("CareNote/1.0"))

        // When
        send(interceptor)
        send(interceptor)

        // Then
        val first = server.takeRequest().headers["X-Request-Id"]
        val second = server.takeRequest().headers["X-Request-Id"]
        assertNotEquals(first, second)
        UUID.fromString(first) // a valid UUID, or this throws
    }

    @Test
    fun `given a request that sets its own headers, when sending it, then they are kept`() {
        // Given
        val request = Request.Builder()
            .url(server.url("/notes"))
            .header("Accept", "text/plain")
            .header("X-Request-Id", "from-caller")
            .build()

        // When
        send(DefaultHeadersInterceptor(ClientInfo("CareNote/1.0")), request)

        // Then
        val headers = server.takeRequest().headers
        assertEquals("text/plain", headers["Accept"])
        assertEquals("from-caller", headers["X-Request-Id"])
    }

    @Test
    fun `given a device name that isn't ascii, when sending, then the request still goes out`() {
        // Given: OkHttp throws for such header values, so this device would fail every request
        val interceptor = DefaultHeadersInterceptor(ClientInfo("CareNote/1.0 (Android 15; Gerät\tÜ)"))

        // When
        send(interceptor)

        // Then
        assertEquals("CareNote/1.0 (Android 15; Ger?t??)", server.takeRequest().headers["User-Agent"])
    }

    private fun send(
        interceptor: DefaultHeadersInterceptor,
        request: Request = Request.Builder().url(server.url("/notes")).build(),
    ) {
        server.enqueue(MockResponse.Builder().code(200).body("{}").build())
        OkHttpClient.Builder().addInterceptor(interceptor).build().newCall(request).execute().close()
    }
}
