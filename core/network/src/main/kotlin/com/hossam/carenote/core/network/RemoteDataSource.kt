package com.hossam.carenote.core.network

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident

/** The server, as the data layer sees it. Never throws for network problems; every outcome is a result. */
interface RemoteDataSource {
    /** Safe to call again for the same note: the note id is the idempotency key. */
    suspend fun sendNote(note: CareNote): SendResult

    suspend fun fetchResidents(): FetchResult<List<Resident>>

    /**
     * The notes the server received after [after], in the order it received them, one page at a time: null for
     * the first page, then the [NotePage.cursor] of the page before. Includes the notes this phone sent itself.
     */
    suspend fun fetchNotes(after: String?): FetchResult<NotePage>
}

/**
 * One page of notes from the server, all of them [com.hossam.carenote.core.model.SyncStatus.SYNCED].
 * [cursor] is opaque: it only means "after this page", and is passed back as is to get the next one.
 */
data class NotePage(val notes: List<CareNote>, val cursor: String, val hasMore: Boolean)

/** What the sync worker needs to know: done, try again later, or give up on this note. */
sealed interface SendResult {
    /** Stored on the server, now or by an earlier attempt. */
    data object Accepted : SendResult

    /** Offline, a timeout, the server is down or busy: the note itself is fine. */
    data object RetryLater : SendResult

    /** The server refused the note itself; retrying the same note won't help. */
    data class Rejected(val code: Int) : SendResult
}

sealed interface FetchResult<out T> {
    data class Success<T>(val value: T) : FetchResult<T>

    /** Offline, a timeout, the server is down or busy: worth trying again later. */
    data object Unavailable : FetchResult<Nothing>

    /** Refused or unreadable: trying again won't help until the app or the server changes. */
    data object Failed : FetchResult<Nothing>
}
