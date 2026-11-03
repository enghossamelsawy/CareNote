package com.hossam.carenote.core.network.mock

import com.hossam.carenote.core.network.IDEMPOTENCY_KEY_HEADER
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Answers every request from [MockServer] instead of the network, so no request leaves the phone.
 * It sits at the end of the interceptor chain, so logging and everything before it run as in prod.
 */
@Singleton
class MockServerInterceptor internal constructor(
    private val server: MockServer,
    private val settings: MockServerSettings,
    private val random: Random,
) : Interceptor {
    @Inject
    constructor(server: MockServer, settings: MockServerSettings) : this(server, settings, Random.Default)

    override fun intercept(chain: Interceptor.Chain): Response {
        val config = settings.config.value
        // An IOException is exactly what OkHttp throws in airplane mode.
        if (config.offline) throw IOException("Mock server: offline")

        // OkHttp runs interceptors on its own threads, so blocking here doesn't block the app.
        Thread.sleep(random.nextLong(config.latencyMillis.first, config.latencyMillis.last + 1))

        val request = chain.request()
        val reply = if (random.nextDouble() < config.failureRate) {
            MockServer.Reply(SERVICE_UNAVAILABLE)
        } else {
            server.handle(
                method = request.method,
                path = request.url.pathSegments.filter { it.isNotEmpty() },
                idempotencyKey = request.header(IDEMPOTENCY_KEY_HEADER),
                body = request.body?.let { body -> Buffer().also(body::writeTo).readUtf8() },
                query = request.url.queryParameterNames.associateWith { request.url.queryParameter(it) },
            )
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(reply.code)
            .message("Mock ${reply.code}")
            .body(reply.body.toResponseBody(JSON))
            .build()
    }

    private companion object {
        const val SERVICE_UNAVAILABLE = 503
        val JSON = "application/json".toMediaType()
    }
}
