package com.hossam.carenote.core.domain.repository

import com.hossam.carenote.core.model.CareNote
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Care notes, stored locally first and synced to the server later.
 * [getNote], [hasRevision], [save] and [resetInterruptedSyncs] throw [StorageException] when the phone's
 * storage fails.
 */
interface CareNoteRepository {
    /** Notes for one resident, newest first. */
    fun observeNotes(residentId: String): Flow<List<CareNote>>

    /** Number of notes not yet synced, per resident id. Residents without such notes are left out. */
    fun observePendingCounts(): Flow<Map<String, Int>>

    /** The note with [noteId], or null if there is none. */
    suspend fun getNote(noteId: UUID): CareNote?

    /** True when a newer note revises [noteId], so it is no longer the current version. */
    suspend fun hasRevision(noteId: UUID): Boolean

    /**
     * Stores the note and queues it for sync in one transaction. Throws [NoteAlreadyRevisedException] when the
     * note is an edit of a version that already has one.
     */
    suspend fun save(note: CareNote)

    /** Queues a FAILED note for another sync attempt; any other note is left as it is and false returned. */
    suspend fun retry(noteId: UUID): Boolean

    /** Notes still marked as syncing from a sync that never finished go back to waiting. */
    suspend fun resetInterruptedSyncs()
}
