package com.hossam.carenote.core.network

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.network.model.NetworkCareNote
import com.hossam.carenote.core.network.model.NetworkResident
import com.hossam.carenote.core.network.model.asExternalModel
import com.hossam.carenote.core.network.model.asNetwork
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.util.UUID

class NetworkJsonTest {
    private val note = CareNote(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        residentId = "res-1",
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.parse("2026-11-01T08:30:00Z"),
    )

    @Test
    fun `given a note, when encoding, then ids and times are sent as text`() {
        // When
        val json = encode(note)

        // Then
        assertEquals(
            Json.parseToJsonElement(
                """
                {
                  "id": "00000000-0000-0000-0000-000000000001",
                  "residentId": "res-1",
                  "transcript": "Pulse 72",
                  "vitals": { "pulse": 72 },
                  "createdAt": "2026-11-01T08:30:00Z"
                }
                """,
            ),
            json,
        )
    }

    @Test
    fun `given a note without some vitals, when encoding, then the missing ones are left out`() {
        // When
        val vitals = encode(note)["vitals"]!!.jsonObject

        // Then
        assertEquals(setOf("pulse"), vitals.keys)
        assertFalse("revisionOf" in encode(note))
    }

    @Test
    fun `given residents with a field the app doesn't know, when decoding, then they still parse`() {
        // Given
        val body = """[{ "id": "res-1", "firstName": "Anna", "lastName": "Becker", "room": "101", "ward": "B" }]"""

        // When
        val residents = careNoteJson.decodeFromString(ListSerializer(NetworkResident.serializer()), body)
            .map { it.asExternalModel() }

        // Then
        assertEquals(listOf(Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")), residents)
    }

    private fun encode(note: CareNote): JsonObject =
        careNoteJson.encodeToJsonElement(NetworkCareNote.serializer(), note.asNetwork()).jsonObject
}
