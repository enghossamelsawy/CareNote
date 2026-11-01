package com.hossam.carenote.core.network

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.network.model.asExternalModel
import com.hossam.carenote.core.network.model.asNetwork
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

    /** The server is down or asks us to slow down: the request itself was fine. */
    private fun Int.isTemporary() = this == TOO_MANY_REQUESTS || this >= SERVER_ERROR

    private companion object {
        const val CONFLICT = 409
        const val TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR = 500
    }
}
