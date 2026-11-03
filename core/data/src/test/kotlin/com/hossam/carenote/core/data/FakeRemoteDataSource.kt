package com.hossam.carenote.core.data

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.network.FetchResult
import com.hossam.carenote.core.network.NotePage
import com.hossam.carenote.core.network.RemoteDataSource
import com.hossam.carenote.core.network.SendResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import java.util.UUID

/** A server the test scripts: say how each note is answered and what the residents and notes calls return. */
class FakeRemoteDataSource : RemoteDataSource {
    /** Answer per note; notes not listed are accepted. */
    val sendResults = mutableMapOf<UUID, SendResult>()
    var residentsResult: FetchResult<List<Resident>> = FetchResult.Success(emptyList())

    /** Every note sent, in order, retries included. */
    val sentNotes = mutableListOf<CareNote>()

    /** When true, a send never answers, like a request still in flight, until it's cancelled. */
    var hangSends = false

    /** Completes once a send has started, so a test can cancel it midway. */
    val sendStarted = CompletableDeferred<Unit>()

    override suspend fun sendNote(note: CareNote): SendResult {
        sentNotes += note
        sendStarted.complete(Unit)
        if (hangSends) awaitCancellation()
        return sendResults[note.id] ?: SendResult.Accepted
    }

    override suspend fun fetchResidents(): FetchResult<List<Resident>> = residentsResult

    /** Answer per cursor, null for the first page; a cursor not listed answers an empty last page. */
    val notePages = mutableMapOf<String?, FetchResult<NotePage>>()

    /** Every cursor notes were fetched after, in order. */
    val fetchedCursors = mutableListOf<String?>()

    override suspend fun fetchNotes(after: String?): FetchResult<NotePage> {
        fetchedCursors += after
        return notePages[after]
            ?: FetchResult.Success(NotePage(notes = emptyList(), cursor = after.orEmpty(), hasMore = false))
    }
}
