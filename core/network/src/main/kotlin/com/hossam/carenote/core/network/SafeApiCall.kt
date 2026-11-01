package com.hossam.carenote.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Every way a call can end, as a value: callers decide what each one means, nothing is thrown at them. */
internal sealed interface NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>

    /** The server answered with a non-2xx status. */
    data class HttpError(val code: Int) : NetworkResult<Nothing>

    /** No network, the server can't be reached, or a timeout. */
    data object NoConnection : NetworkResult<Nothing>

    /** The server answered 2xx with a body we can't read. */
    data object BadResponse : NetworkResult<Nothing>
}

/**
 * Runs one network call and turns whatever it throws into a [NetworkResult].
 * Cancellation is rethrown, never swallowed, so leaving a screen or stopping a worker still works.
 */
internal suspend fun <T> safeApiCall(call: suspend () -> T): NetworkResult<T> = try {
    NetworkResult.Success(call())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: HttpException) {
    NetworkResult.HttpError(error.code())
} catch (error: IOException) {
    NetworkResult.NoConnection
} catch (error: SerializationException) {
    NetworkResult.BadResponse
}

/** Calls returning [Response] don't throw for non-2xx statuses; this makes them behave like the others. */
internal fun <T> Response<T>.orThrow(): Response<T> = if (isSuccessful) this else throw HttpException(this)
