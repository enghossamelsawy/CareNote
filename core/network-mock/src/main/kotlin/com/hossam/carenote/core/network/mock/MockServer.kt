package com.hossam.carenote.core.network.mock

import com.hossam.carenote.core.network.careNoteJson
import com.hossam.carenote.core.network.model.NetworkCareNote
import com.hossam.carenote.core.network.model.NetworkNotePage
import com.hossam.carenote.core.network.model.NetworkResident
import com.hossam.carenote.core.network.model.NetworkVitals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An in-memory CareNote server with the same contract as the real one, so the whole network stack
 * (Retrofit, JSON, error mapping, logging) runs in the mock flavor too. Kept apart from OkHttp so the
 * rules can be tested directly.
 */
@Singleton
class MockServer @Inject constructor(private val clock: Clock) {
    /** Guards [notesByKey] and [received], which change together. OkHttp calls in from several threads. */
    private val lock = Any()
    private val notesByKey = mutableMapOf<String, NetworkCareNote>()

    /** In the order they arrived. A note's position here is its place in the `GET notes` feed. */
    private val received = mutableListOf<NetworkCareNote>()
    private val _storedNotes = MutableStateFlow<List<NetworkCareNote>>(emptyList())

    /** Every note stored so far, oldest first, for the debug menu. */
    val storedNotes: StateFlow<List<NetworkCareNote>> = _storedNotes.asStateFlow()

    internal data class Reply(val code: Int, val body: String = "")

    internal fun handle(
        method: String,
        path: List<String>,
        idempotencyKey: String?,
        body: String?,
        query: Map<String, String?> = emptyMap(),
    ): Reply = when {
        method == "POST" && path == listOf("notes") -> storeNote(idempotencyKey, body)
        method == "GET" && path == listOf("notes") -> notesPage(after = query["after"], limit = query["limit"])
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
        return if (store(note)) Reply(CREATED) else Reply(CONFLICT)
    }

    /**
     * Stores a note as if a colleague's phone had sent it, for the debug menu: this phone gets it on its next
     * pull, like a real colleague's note. Written now, for [residentId] or a random made-up resident.
     */
    fun addNoteFromAnotherPhone(residentId: String = residents.random().id): NetworkCareNote {
        val note = NetworkCareNote(
            id = UUID.randomUUID().toString(),
            residentId = residentId,
            transcript = "Written on another phone. Pulse 76.",
            vitals = NetworkVitals(pulse = 76),
            createdAt = clock.instant().toString(),
        )
        store(note)
        return note
    }

    /** False if a note with this id is already stored. */
    private fun store(note: NetworkCareNote): Boolean = synchronized(lock) {
        if (note.id in notesByKey) return false
        notesByKey[note.id] = note
        received += note
        _storedNotes.value = notesByKey.values.sortedBy { it.createdAt }
        true
    }

    /**
     * At most [limit] notes received after [after]; without [after], from the first one. The cursor here is how
     * many notes were received up to the page's end, but the app never reads it, it only sends it back.
     * A cursor this server could never give out, or a missing or too large limit, is a bad request.
     *
     * A cursor past the last note comes from before a restart: this server forgets its notes when the app stops,
     * but the phone keeps its cursor. It gets an empty last page, with the cursor moved back to the current end.
     */
    private fun notesPage(after: String?, limit: String?): Reply {
        val pageSize = limit?.toIntOrNull()?.takeIf { it in 1..MAX_PAGE_SIZE } ?: return Reply(BAD_REQUEST)
        val requestedStart = if (after == null) 0 else after.toIntOrNull() ?: return Reply(BAD_REQUEST)
        if (requestedStart < 0) return Reply(BAD_REQUEST)
        val page = synchronized(lock) {
            val start = minOf(requestedStart, received.size)
            val end = minOf(start + pageSize, received.size)
            NetworkNotePage(
                notes = received.subList(start, end).toList(),
                cursor = end.toString(),
                hasMore = end < received.size,
            )
        }
        return Reply(OK, careNoteJson.encodeToString(NetworkNotePage.serializer(), page))
    }

    private val residentsJson = careNoteJson.encodeToString(ListSerializer(NetworkResident.serializer()), residents)

    internal companion object {
        const val OK = 200
        const val CREATED = 201
        const val BAD_REQUEST = 400
        const val NOT_FOUND = 404
        const val CONFLICT = 409

        /** The most notes one page may ask for; the app asks for 100. */
        const val MAX_PAGE_SIZE = 500

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
