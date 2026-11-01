package com.hossam.carenote.core.domain.note

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class NoteVersionsTest {
    @Test
    fun `given two notes that point at each other, when following them, then it stops instead of looping`() {
        // Given
        val first = note(id = 1, revisionOf = UUID(0, 2))
        val second = note(id = 2, revisionOf = UUID(0, 1))
        val versions = NoteVersions(listOf(first, second))

        // When
        val earlier = versions.earlier(first)
        val latest = versions.latest(first.id)

        // Then
        assertEquals(listOf(second), earlier)
        assertEquals(second, latest)
    }

    @Test
    fun `given an edit whose original is missing, when listing earlier versions, then there are none`() {
        // Given
        val edit = note(id = 2, revisionOf = UUID(0, 1))
        val versions = NoteVersions(listOf(edit))

        // When
        val earlier = versions.earlier(edit)

        // Then
        assertEquals(emptyList<CareNote>(), earlier)
        assertEquals(listOf(edit), versions.current)
    }

    private fun note(id: Long, revisionOf: UUID?) = CareNote(
        id = UUID(0, id),
        residentId = "res-1",
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.ofEpochSecond(id),
        revisionOf = revisionOf,
    )
}
