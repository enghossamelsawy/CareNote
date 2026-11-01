package com.hossam.carenote.core.data.repository

import com.hossam.carenote.core.data.mapper.asEntity
import com.hossam.carenote.core.data.mapper.asExternalModel
import com.hossam.carenote.core.data.mapper.asPendingCountMap
import com.hossam.carenote.core.database.dao.CareNoteDao
import com.hossam.carenote.core.database.model.OutboxEntity
import com.hossam.carenote.core.domain.repository.CareNoteRepository
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

    override suspend fun getNote(noteId: UUID): CareNote? = careNoteDao.getById(noteId.toString())?.asExternalModel()

    override suspend fun hasRevision(noteId: UUID): Boolean = careNoteDao.hasRevision(noteId.toString())

    override suspend fun save(note: CareNote) {
        careNoteDao.insertAndQueue(note.asEntity(), outboxEntry(note.id))
    }

    override suspend fun retry(noteId: UUID) {
        careNoteDao.requeue(outboxEntry(noteId))
    }

    private fun outboxEntry(noteId: UUID) = OutboxEntity(noteId = noteId.toString(), enqueuedAt = clock.instant())
}
