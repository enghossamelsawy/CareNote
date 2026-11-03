package com.hossam.carenote.core.data.repository

import com.hossam.carenote.core.database.dao.NoteFeedDao
import com.hossam.carenote.core.domain.repository.NoteFeedRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

/** Reads what [com.hossam.carenote.core.data.sync.NotePuller] stores; only the puller writes it. */
internal class RoomNoteFeedRepository @Inject constructor(private val noteFeedDao: NoteFeedDao) : NoteFeedRepository {
    override fun observeCaughtUpAt(): Flow<Instant?> = noteFeedDao.observeNotesCaughtUpAt()
}
