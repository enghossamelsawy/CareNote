package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.common.logging.Logger
import com.hossam.carenote.core.data.mapper.asEntity
import com.hossam.carenote.core.database.dao.NoteFeedDao
import com.hossam.carenote.core.domain.repository.RefreshResult
import com.hossam.carenote.core.domain.repository.ResidentRepository
import com.hossam.carenote.core.network.FetchResult
import com.hossam.carenote.core.network.RemoteDataSource
import java.time.Clock
import javax.inject.Inject

/**
 * Brings other phones' notes to this one: reads the server's notes feed from where the last pull stopped,
 * page by page, until it is caught up. The screens read the database, so pulled notes appear by themselves.
 *
 * - Residents are refreshed first. A note for a resident the phone doesn't know can't be stored, and once
 *   the cursor has moved past it, it is never fetched again; so without fresh residents, nothing is pulled.
 * - Each page is stored with its cursor in one transaction, so an interrupted pull continues where it was.
 * - Pulling only adds notes, see [NoteFeedDao.insertPulled]; it never changes or removes one.
 */
internal class NotePuller @Inject constructor(
    private val residentRepository: ResidentRepository,
    private val remoteDataSource: RemoteDataSource,
    private val noteFeedDao: NoteFeedDao,
    private val clock: Clock,
    private val logger: Logger,
) {
    suspend fun pull(): SyncOutcome {
        when (residentRepository.refresh()) {
            RefreshResult.Updated -> Unit
            RefreshResult.Unavailable -> return SyncOutcome.RetryLater
            RefreshResult.Failed -> return SyncOutcome.Failed
        }
        var after = noteFeedDao.notesCursor()?.cursor
        do {
            val page = when (val result = remoteDataSource.fetchNotes(after)) {
                is FetchResult.Success -> result.value
                FetchResult.Unavailable -> return SyncOutcome.RetryLater
                FetchResult.Failed -> return SyncOutcome.Failed
            }
            // A server that says "more" without moving on would keep this loop going forever.
            if (page.hasMore && page.cursor == after) return SyncOutcome.Failed
            val skipped = noteFeedDao.insertPulled(
                notes = page.notes.map { it.asEntity() },
                cursor = page.cursor,
                caughtUpAt = if (page.hasMore) null else clock.instant(),
            )
            // Ids only: a note's text is health data.
            if (skipped.isNotEmpty()) logger.w(TAG, "Skipped notes for unknown residents: $skipped")
            after = page.cursor
        } while (page.hasMore)
        return SyncOutcome.Done
    }

    private companion object {
        const val TAG = "NotePuller"
    }
}
