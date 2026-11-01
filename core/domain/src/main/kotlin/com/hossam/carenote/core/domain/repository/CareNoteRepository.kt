package com.hossam.carenote.core.domain.repository

import com.hossam.carenote.core.model.CareNote
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Care notes, stored locally first and synced to the server later. */
interface CareNoteRepository {
    /** Notes for one resident, newest first. */
    fun observeNotes(residentId: String): Flow<List<CareNote>>

    /** Number of notes not yet synced, per resident id. Residents without such notes are left out. */
    fun observePendingCounts(): Flow<Map<String, Int>>

    /** The note with [noteId], or null if there is none. */
    suspend fun getNote(noteId: UUID): CareNote?

    /** True when a newer note revises [noteId], so it is no longer the current version. */
    suspend fun hasRevision(noteId: UUID): Boolean

    /** Stores the note and queues it for sync in one transaction. */
    suspend fun save(note: CareNote)

    /** Queues a FAILED note for another sync attempt. */
    suspend fun retry(noteId: UUID)
}
