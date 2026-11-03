package com.hossam.carenote.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * Adds the headers every request carries:
 * - `Accept: application/json`;
 * - `User-Agent`, so server logs show the app version and device;
 * - `X-Request-Id`, a new id per request, to find one failing request in the server logs. A note sent again
 *   keeps its [IDEMPOTENCY_KEY_HEADER] but gets a new request id, so each attempt can be traced.
 *
 * Headers a request already sets are kept. The user agent is reduced to printable ASCII, because OkHttp
 * rejects other characters in header values, and device names can contain them.
 */
internal class DefaultHeadersInterceptor(
    clientInfo: ClientInfo,
    private val newRequestId: () -> String = { UUID.randomUUID().toString() },
) : Interceptor {
    private val userAgent = clientInfo.userAgent.map { if (it in PRINTABLE_ASCII) it else '?' }.joinToString("")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
        if (request.header(ACCEPT) == null) builder.header(ACCEPT, "application/json")
        if (request.header(USER_AGENT) == null) builder.header(USER_AGENT, userAgent)
        if (request.header(REQUEST_ID) == null) builder.header(REQUEST_ID, newRequestId())
        return chain.proceed(builder.build())
    }

    companion object {
        const val ACCEPT = "Accept"
        const val USER_AGENT = "User-Agent"
        const val REQUEST_ID = "X-Request-Id"

        private val PRINTABLE_ASCII = ' '..'~'
    }
}
