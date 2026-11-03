package com.hossam.carenote.core.network.mock

import com.hossam.carenote.core.network.careNoteJson
import com.hossam.carenote.core.network.model.NetworkCareNote
import com.hossam.carenote.core.network.model.NetworkNotePage
import com.hossam.carenote.core.network.model.NetworkResident
import com.hossam.carenote.core.network.model.NetworkVitals
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Test

class MockServerTest {
    private val server = MockServer()

    private val note = NetworkCareNote(
        id = "00000000-0000-0000-0000-000000000001",
        residentId = "res-101",
        transcript = "Pulse 72",
        vitals = NetworkVitals(pulse = 72),
        createdAt = "2026-11-01T08:30:00Z",
    )
    private val noteJson = careNoteJson.encodeToString(NetworkCareNote.serializer(), note)

    @Test
    fun `given a new note, when it is posted, then it is stored`() {
        // When
        val reply = postNote(idempotencyKey = note.id, body = noteJson)

        // Then
        assertEquals(MockServer.CREATED, reply.code)
        assertEquals(listOf(note), server.storedNotes.value)
    }

    @Test
    fun `given a stored note, when it is posted again with the same key, then it conflicts and is stored once`() {
        // Given
        postNote(idempotencyKey = note.id, body = noteJson)

        // When
        val reply = postNote(idempotencyKey = note.id, body = noteJson)

        // Then
        assertEquals(MockServer.CONFLICT, reply.code)
        assertEquals(1, server.storedNotes.value.size)
    }

    @Test
    fun `given a note without an idempotency key, when it is posted, then it is a bad request`() {
        // When
        val reply = postNote(idempotencyKey = null, body = noteJson)

        // Then
        assertEquals(MockServer.BAD_REQUEST, reply.code)
    }

    @Test
    fun `given a body that is not a note, when it is posted, then it is a bad request`() {
        // When
        val reply = postNote(idempotencyKey = note.id, body = """{ "hello": "world" }""")

        // Then
        assertEquals(MockServer.BAD_REQUEST, reply.code)
    }

    @Test
    fun `given the server, when residents are requested, then the made-up residents are returned`() {
        // When
        val reply = server.handle(method = "GET", path = listOf("residents"), idempotencyKey = null, body = null)

        // Then
        assertEquals(MockServer.OK, reply.code)
        assertEquals(
            MockServer.residents,
            careNoteJson.decodeFromString(ListSerializer(NetworkResident.serializer()), reply.body),
        )
    }

    @Test
    fun `given the server, when an unknown path is requested, then it is not found`() {
        // When
        val reply = server.handle(method = "GET", path = listOf("unknown"), idempotencyKey = null, body = null)

        // Then
        assertEquals(MockServer.NOT_FOUND, reply.code)
    }

    @Test
    fun `given no notes, when the first page is requested, then it is empty and last`() {
        // When
        val page = notesPage(after = null, limit = "100")

        // Then
        assertEquals(NetworkNotePage(notes = emptyList(), cursor = "0", hasMore = false), page)
    }

    @Test
    fun `given three notes, when they are requested two at a time, then the cursor walks through them`() {
        // Given
        val notes = (1..3).map { numberedNote(id = it) }
        notes.forEach { post(it) }

        // When
        val first = notesPage(after = null, limit = "2")
        val second = notesPage(after = first.cursor, limit = "2")

        // Then
        assertEquals(NetworkNotePage(notes = notes.take(2), cursor = "2", hasMore = true), first)
        assertEquals(NetworkNotePage(notes = notes.drop(2), cursor = "3", hasMore = false), second)
    }

    @Test
    fun `given a note written earlier but received later, when notes are requested, then it comes last`() {
        // Given: written offline yesterday, so it reaches the server after today's note
        val today = numberedNote(id = 1, createdAt = "2026-11-02T08:00:00Z")
        val yesterday = numberedNote(id = 2, createdAt = "2026-11-01T08:00:00Z")
        post(today)
        val cursor = notesPage(after = null, limit = "100").cursor
        post(yesterday)

        // When
        val page = notesPage(after = cursor, limit = "100")

        // Then
        assertEquals(listOf(yesterday), page.notes)
    }

    @Test
    fun `given a cursor the server never gave out, when notes are requested, then it is a bad request`() {
        // When
        val replies = listOf("abc", "-1", "5").map { getNotes(query = mapOf("after" to it, "limit" to "100")) }

        // Then
        assertEquals(List(3) { MockServer.BAD_REQUEST }, replies.map { it.code })
    }

    @Test
    fun `given no limit or one too large, when notes are requested, then it is a bad request`() {
        // When
        val replies = listOf(emptyMap(), mapOf("limit" to "0"), mapOf("limit" to "501")).map { getNotes(it) }

        // Then
        assertEquals(List(3) { MockServer.BAD_REQUEST }, replies.map { it.code })
    }

    private fun postNote(idempotencyKey: String?, body: String) =
        server.handle(method = "POST", path = listOf("notes"), idempotencyKey = idempotencyKey, body = body)

    private fun post(note: NetworkCareNote) =
        postNote(idempotencyKey = note.id, body = careNoteJson.encodeToString(NetworkCareNote.serializer(), note))

    private fun getNotes(query: Map<String, String?>) =
        server.handle(method = "GET", path = listOf("notes"), idempotencyKey = null, body = null, query = query)

    private fun notesPage(after: String?, limit: String): NetworkNotePage {
        val reply = getNotes(query = mapOf("after" to after, "limit" to limit))
        assertEquals(MockServer.OK, reply.code)
        return careNoteJson.decodeFromString(NetworkNotePage.serializer(), reply.body)
    }

    private fun numberedNote(id: Int, createdAt: String = "2026-11-01T08:30:00Z") = note.copy(
        id = "00000000-0000-0000-0000-00000000000$id",
        createdAt = createdAt,
    )
}
