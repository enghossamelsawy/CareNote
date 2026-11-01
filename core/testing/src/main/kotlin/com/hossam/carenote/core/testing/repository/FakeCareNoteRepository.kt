package com.hossam.carenote.core.testing.repository

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.StorageException
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
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

    /** Every note saved so far, in the order they were saved. */
    val savedNotes: List<CareNote>
        get() = notes.value

    fun setSyncStatus(noteId: UUID, status: SyncStatus) {
        notes.update { list -> list.map { if (it.id == noteId) it.copy(syncStatus = status) else it } }
    }

    override fun observeNotes(residentId: String): Flow<List<CareNote>> =
        notes.map { list -> list.filter { it.residentId == residentId }.sortedByDescending { it.createdAt } }

    override fun observePendingCounts(): Flow<Map<String, Int>> = notes.map { list ->
        list.filter { it.syncStatus != SyncStatus.SYNCED }.groupingBy { it.residentId }.eachCount()
    }

    override suspend fun getNote(noteId: UUID): CareNote? = notes.value.find { it.id == noteId }

    override suspend fun hasRevision(noteId: UUID): Boolean = notes.value.any { it.revisionOf == noteId }

    override suspend fun save(note: CareNote) {
        if (failNextSave) {
            failNextSave = false
            throw StorageException(IllegalStateException("Disk full"))
        }
        check(notes.value.none { it.id == note.id }) { "Notes are append-only; ${note.id} already exists" }
        notes.update { it + note }
    }

    override suspend fun retry(noteId: UUID) {
        setSyncStatus(noteId, SyncStatus.PENDING)
    }
}
