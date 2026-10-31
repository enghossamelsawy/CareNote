package com.hossam.carenote.core.data.mapper

import com.hossam.carenote.core.database.model.PendingCount
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class CareNoteMapperTest {
    @Test
    fun `given a note with every field set, when mapping to an entity and back, then it is unchanged`() {
        // Given
        val note = careNote(
            vitals = Vitals(
                systolic = 120,
                diastolic = 80,
                pulse = 64,
                temperatureCelsius = 36.8,
                oxygenSaturation = 98,
            ),
            revisionOf = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            syncStatus = SyncStatus.FAILED,
        )

        // When
        val roundTrip = note.asEntity().asExternalModel()

        // Then
        assertEquals(note, roundTrip)
    }

    @Test
    fun `given a note without vitals or revision, when mapping to an entity and back, then the nulls are kept`() {
        // Given
        val note = careNote(vitals = Vitals.EMPTY, revisionOf = null)

        // When
        val roundTrip = note.asEntity().asExternalModel()

        // Then
        assertEquals(note, roundTrip)
    }

    @Test
    fun `given a note, when mapping to an entity, then the ids are stored as UUID strings`() {
        // Given
        val note = careNote(revisionOf = UUID.fromString("00000000-0000-0000-0000-000000000001"))

        // When
        val entity = note.asEntity()

        // Then
        assertEquals("3f2a8c1e-0d4b-4c6a-9e7f-1a2b3c4d5e6f", entity.id)
        assertEquals("00000000-0000-0000-0000-000000000001", entity.revisionOf)
    }

    @Test
    fun `given pending counts, when mapping to a map, then each resident maps to their count`() {
        // Given
        val counts = listOf(PendingCount("res-101", 3), PendingCount("res-102", 1))

        // When
        val map = counts.asPendingCountMap()

        // Then
        assertEquals(mapOf("res-101" to 3, "res-102" to 1), map)
    }

    private fun careNote(
        vitals: Vitals = Vitals(pulse = 72),
        revisionOf: UUID? = null,
        syncStatus: SyncStatus = SyncStatus.PENDING,
    ) = CareNote(
        id = UUID.fromString("3f2a8c1e-0d4b-4c6a-9e7f-1a2b3c4d5e6f"),
        residentId = "res-101",
        transcript = "Pulse 72",
        vitals = vitals,
        createdAt = Instant.ofEpochSecond(1_700_000_000),
        revisionOf = revisionOf,
        syncStatus = syncStatus,
    )
}
