package com.hossam.carenote.core.network.mock

import com.hossam.carenote.core.network.careNoteJson
import com.hossam.carenote.core.network.model.NetworkCareNote
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
        assertEquals(listOf(note), server.storedNotes)
    }

    @Test
    fun `given a stored note, when it is posted again with the same key, then it conflicts and is stored once`() {
        // Given
        postNote(idempotencyKey = note.id, body = noteJson)

        // When
        val reply = postNote(idempotencyKey = note.id, body = noteJson)

        // Then
        assertEquals(MockServer.CONFLICT, reply.code)
        assertEquals(1, server.storedNotes.size)
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

    private fun postNote(idempotencyKey: String?, body: String) =
        server.handle(method = "POST", path = listOf("notes"), idempotencyKey = idempotencyKey, body = body)
}
