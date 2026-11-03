package com.hossam.carenote.core.testing.repository

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.NoteAlreadyRevisedException
import com.hossam.carenote.core.domain.repository.StorageException
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/** In-memory [CareNoteRepository] that keeps the real one's contract: append-only, newest first. */
class FakeCareNoteRepository : CareNoteRepository {
    private val notes = MutableStateFlow<List<CareNote>>(emptyList())

    /** When true, the next [save] fails like a full disk would, and stores nothing. */
    var failNextSave = false

    /** When true, the next [hasRevision] answers false, like a check made just before another edit was saved. */
    var missNextRevision = false

    /** While set and not completed, [getNote] waits on it, like a slow read from the database. */
    var getNoteGate: CompletableDeferred<Unit>? = null

    /** Every note saved so far, in the order they were saved. */
    val savedNotes: List<CareNote>
        get() = notes.value

    fun setSyncStatus(noteId: UUID, status: SyncStatus) {
        notes.update { list -> list.map { if (it.id == noteId) it.copy(syncStatus = status) else it } }
    }

    override fun observeNotes(residentId: String): Flow<List<CareNote>> =
        notes.map { list -> list.filter { it.residentId == residentId }.sortedByDescending { it.createdAt } }

    override fun observeAllNotes(): Flow<List<CareNote>> =
        notes.map { list -> list.sortedByDescending { it.createdAt } }

    override fun observePendingCounts(): Flow<Map<String, Int>> = notes.map { list ->
        list.filter { it.syncStatus != SyncStatus.SYNCED }.groupingBy { it.residentId }.eachCount()
    }

    override suspend fun getNote(noteId: UUID): CareNote? {
        getNoteGate?.await()
        return notes.value.find { it.id == noteId }
    }

    override suspend fun hasRevision(noteId: UUID): Boolean {
        if (missNextRevision) {
            missNextRevision = false
            return false
        }
        return notes.value.any { it.revisionOf == noteId }
    }

    override suspend fun save(note: CareNote) {
        if (failNextSave) {
            failNextSave = false
            throw StorageException(IllegalStateException("Disk full"))
        }
        check(notes.value.none { it.id == note.id }) { "Notes are append-only; ${note.id} already exists" }
        val original = note.revisionOf
        // Like the database's unique index: one edit per version.
        if (original != null && notes.value.any { it.revisionOf == original }) {
            throw NoteAlreadyRevisedException(original)
        }
        notes.update { it + note }
    }

    override suspend fun retry(noteId: UUID): Boolean {
        if (notes.value.none { it.id == noteId && it.syncStatus == SyncStatus.FAILED }) return false
        setSyncStatus(noteId, SyncStatus.PENDING)
        return true
    }

    override suspend fun resetInterruptedSyncs() {
        notes.update { list ->
            list.map { if (it.syncStatus == SyncStatus.SYNCING) it.copy(syncStatus = SyncStatus.PENDING) else it }
        }
    }
}
