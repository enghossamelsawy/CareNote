package com.hossam.carenote.core.network

import com.hossam.carenote.core.network.model.NetworkCareNote
import com.hossam.carenote.core.network.model.NetworkResident
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Sent with every note, set to the note's id. Retrying with the same key never stores the note twice:
 * the server answers 409 Conflict instead.
 */
const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

internal interface CareNoteApi {
    @POST("notes")
    suspend fun sendNote(
        @Header(IDEMPOTENCY_KEY_HEADER) idempotencyKey: String,
        @Body note: NetworkCareNote,
    ): Response<Unit>

    @GET("residents")
    suspend fun residents(): List<NetworkResident>
}

/** Used by the Hilt module and by tests, so both build the API the same way. */
internal fun careNoteApi(baseUrl: HttpUrl, client: OkHttpClient): CareNoteApi = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(careNoteJson.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(CareNoteApi::class.java)
