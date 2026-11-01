package com.hossam.carenote.core.network.mock

import com.hossam.carenote.core.network.careNoteJson
import com.hossam.carenote.core.network.model.NetworkCareNote
import com.hossam.carenote.core.network.model.NetworkResident
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An in-memory CareNote server with the same contract as the real one, so the whole network stack
 * (Retrofit, JSON, error mapping, logging) runs in the mock flavor too. Kept apart from OkHttp so the
 * rules can be tested directly.
 */
@Singleton
class MockServer @Inject constructor() {
    private val notesByKey = ConcurrentHashMap<String, NetworkCareNote>()

    /** Every note stored so far, for the debug menu. */
    val storedNotes: List<NetworkCareNote>
        get() = notesByKey.values.sortedBy { it.createdAt }

    internal data class Reply(val code: Int, val body: String = "")

    internal fun handle(method: String, path: List<String>, idempotencyKey: String?, body: String?): Reply = when {
        method == "POST" && path == listOf("notes") -> storeNote(idempotencyKey, body)
        method == "GET" && path == listOf("residents") -> Reply(OK, residentsJson)
        else -> Reply(NOT_FOUND)
    }

    /** Like the real server: the idempotency key makes a retried note a no-op answered with 409. */
    private fun storeNote(idempotencyKey: String?, body: String?): Reply {
        if (idempotencyKey.isNullOrBlank() || body == null) return Reply(BAD_REQUEST)
        val note = try {
            careNoteJson.decodeFromString(NetworkCareNote.serializer(), body)
        } catch (error: SerializationException) {
            return Reply(BAD_REQUEST)
        }
        if (note.id != idempotencyKey) return Reply(BAD_REQUEST)
        return if (notesByKey.putIfAbsent(idempotencyKey, note) == null) Reply(CREATED) else Reply(CONFLICT)
    }

    private val residentsJson = careNoteJson.encodeToString(ListSerializer(NetworkResident.serializer()), residents)

    internal companion object {
        const val OK = 200
        const val CREATED = 201
        const val BAD_REQUEST = 400
        const val NOT_FOUND = 404
        const val CONFLICT = 409

        /** Made-up people; the app has never seen real resident data. */
        val residents = listOf(
            NetworkResident(id = "res-101", firstName = "Anna", lastName = "Becker", room = "101"),
            NetworkResident(id = "res-102", firstName = "Karl", lastName = "Fischer", room = "102"),
            NetworkResident(id = "res-103", firstName = "Greta", lastName = "Hoffmann", room = "103"),
            NetworkResident(id = "res-104", firstName = "Hans", lastName = "Klein", room = "104"),
            NetworkResident(id = "res-105", firstName = "Ingrid", lastName = "Lang", room = "105"),
            NetworkResident(id = "res-106", firstName = "Otto", lastName = "Neumann", room = "106"),
            NetworkResident(id = "res-107", firstName = "Rosa", lastName = "Schulz", room = "107"),
            NetworkResident(id = "res-108", firstName = "Walter", lastName = "Vogel", room = "108"),
        )
    }
}
