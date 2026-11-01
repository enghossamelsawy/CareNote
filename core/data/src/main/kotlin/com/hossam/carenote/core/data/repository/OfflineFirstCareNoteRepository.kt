package com.hossam.carenote.core.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import com.hossam.carenote.core.data.mapper.asEntity
import com.hossam.carenote.core.data.mapper.asExternalModel
import com.hossam.carenote.core.data.mapper.asPendingCountMap
import com.hossam.carenote.core.database.dao.CareNoteDao
import com.hossam.carenote.core.database.model.OutboxEntity
import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.NoteAlreadyRevisedException
import com.hossam.carenote.core.domain.repository.StorageException
import com.hossam.carenote.core.model.CareNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Notes are written to the local database first, together with an outbox entry; the sync worker
 * sends them later. Nothing here waits for the network.
 */
internal class OfflineFirstCareNoteRepository @Inject constructor(
    private val careNoteDao: CareNoteDao,
    private val clock: Clock,
) : CareNoteRepository {
    override fun observeNotes(residentId: String): Flow<List<CareNote>> =
        careNoteDao.observeForResident(residentId).map { notes -> notes.map { it.asExternalModel() } }

    override fun observePendingCounts(): Flow<Map<String, Int>> =
        careNoteDao.observePendingCounts().map { it.asPendingCountMap() }

    override suspend fun getNote(noteId: UUID): CareNote? = storage {
        careNoteDao.getById(noteId.toString())?.asExternalModel()
    }

    override suspend fun hasRevision(noteId: UUID): Boolean = storage { careNoteDao.hasRevision(noteId.toString()) }

    override suspend fun save(note: CareNote) {
        try {
            storage { careNoteDao.insertAndQueue(note.asEntity(), outboxEntry(note.id)) }
        } catch (error: StorageException) {
            val original = note.revisionOf
            // The unique index on revisionOf refused a second edit of the same version.
            if (original != null && error.cause is SQLiteConstraintException && hasRevision(original)) {
                throw NoteAlreadyRevisedException(original)
            }
            throw error
        }
    }

    override suspend fun retry(noteId: UUID): Boolean = storage { careNoteDao.requeue(outboxEntry(noteId)) }

    override suspend fun resetInterruptedSyncs() {
        storage { careNoteDao.resetSyncing() }
    }

    /** Only database errors become [StorageException]; anything else is a bug and stays as it is. */
    private suspend fun <T> storage(block: suspend () -> T): T = try {
        block()
    } catch (error: SQLiteException) {
        throw StorageException(error)
    }

    private fun outboxEntry(noteId: UUID) = OutboxEntity(noteId = noteId.toString(), enqueuedAt = clock.instant())
}
