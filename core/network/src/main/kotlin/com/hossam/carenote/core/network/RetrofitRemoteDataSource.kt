package com.hossam.carenote.core.network

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.network.model.NetworkNotePage
import com.hossam.carenote.core.network.model.asExternalModel
import com.hossam.carenote.core.network.model.asNetwork
import java.time.format.DateTimeParseException
import javax.inject.Inject

internal class RetrofitRemoteDataSource @Inject constructor(private val api: CareNoteApi) : RemoteDataSource {
    override suspend fun sendNote(note: CareNote): SendResult =
        when (val result = safeApiCall { api.sendNote(note.id.toString(), note.asNetwork()).orThrow() }) {
            is NetworkResult.Success -> SendResult.Accepted
            is NetworkResult.HttpError -> when {
                // Already stored under this idempotency key by an earlier attempt.
                result.code == CONFLICT -> SendResult.Accepted
                result.code.isTemporary() -> SendResult.RetryLater
                else -> SendResult.Rejected(result.code)
            }

            NetworkResult.NoConnection, NetworkResult.BadResponse -> SendResult.RetryLater
        }

    override suspend fun fetchResidents(): FetchResult<List<Resident>> =
        when (val result = safeApiCall { api.residents() }) {
            is NetworkResult.Success -> FetchResult.Success(result.value.map { it.asExternalModel() })
            is NetworkResult.HttpError -> if (result.code.isTemporary()) FetchResult.Unavailable else FetchResult.Failed
            NetworkResult.NoConnection -> FetchResult.Unavailable
            NetworkResult.BadResponse -> FetchResult.Failed
        }

    override suspend fun fetchNotes(after: String?): FetchResult<NotePage> =
        when (val result = safeApiCall { api.notes(after = after, limit = PAGE_SIZE) }) {
            is NetworkResult.Success -> result.value.asFetchResult()
            is NetworkResult.HttpError -> if (result.code.isTemporary()) FetchResult.Unavailable else FetchResult.Failed
            NetworkResult.NoConnection -> FetchResult.Unavailable
            NetworkResult.BadResponse -> FetchResult.Failed
        }

    /**
     * Failed when a note's id or time can't be read. The whole page then counts as unreadable, like a body that
     * isn't JSON: skipping the note would move the cursor past it, and it would never be fetched again.
     */
    private fun NetworkNotePage.asFetchResult(): FetchResult<NotePage> = try {
        FetchResult.Success(asExternalModel())
    } catch (error: IllegalArgumentException) {
        FetchResult.Failed
    } catch (error: DateTimeParseException) {
        FetchResult.Failed
    }

    /**
     * The request timed out, the server is down, or it asks us to slow down: the request itself was fine.
     * 401 and 403 count as refused for now; once there is a login, an expired one must not fail every note.
     */
    private fun Int.isTemporary() = this == REQUEST_TIMEOUT || this == TOO_MANY_REQUESTS || this >= SERVER_ERROR

    private companion object {
        const val REQUEST_TIMEOUT = 408
        const val CONFLICT = 409
        const val TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR = 500

        /** Notes per page: small enough for a slow connection, large enough that a day's notes take few calls. */
        const val PAGE_SIZE = 100
    }
}
