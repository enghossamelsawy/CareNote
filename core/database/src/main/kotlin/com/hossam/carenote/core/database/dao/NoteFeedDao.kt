package com.hossam.carenote.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.hossam.carenote.core.database.model.CareNoteEntity
import com.hossam.carenote.core.database.model.SyncCursorEntity
import com.hossam.carenote.core.database.model.SyncCursorEntity.Companion.NOTES_FEED
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Notes pulled from the server, and how far the phone has read the server's notes feed. */
@Dao
interface NoteFeedDao {
    @Query("SELECT * FROM sync_cursors WHERE feed = '$NOTES_FEED'")
    suspend fun notesCursor(): SyncCursorEntity?

    /** See [SyncCursorEntity.caughtUpAt]; null too while there is no cursor yet. */
    @Query("SELECT caughtUpAt FROM sync_cursors WHERE feed = '$NOTES_FEED'")
    fun observeNotesCaughtUpAt(): Flow<Instant?>

    /**
     * Stores one page of pulled notes and moves the cursor past it, in one transaction: if the app is killed
     * midway, neither happened, and the next pull fetches the same page again.
     *
     * Only adds notes. One the phone already has, such as a note it sent itself, is left exactly as it is, and
     * so is anything else stored here. A note for a resident the phone doesn't know is skipped, because the
     * foreign key would refuse it and with it the whole page, again on every pull.
     *
     * Known gap: another phone's edit of a version this phone has already edited is dropped too, since a
     * version can have only one edit. Two phones editing the same note needs the server to refuse the second.
     * Those ids are returned, so the gap can be traced; an edit of a dropped edit then has no original here.
     *
     * @param caughtUpAt set when this was the feed's last page; null keeps the previous time.
     */
    @Transaction
    suspend fun insertPulled(notes: List<CareNoteEntity>, cursor: String, caughtUpAt: Instant?): PulledPage {
        val knownResidents = knownResidentIds(notes.map { it.residentId }.distinct()).toSet()
        val (known, unknown) = notes.partition { it.residentId in knownResidents }
        insertIgnoringExisting(known)
        // Unknown residents are filtered out above, so a note still missing was refused by the one-edit index.
        val stored = storedNoteIds(known.map { it.id }).toSet()
        val previous = notesCursor()
        upsertCursor(SyncCursorEntity(NOTES_FEED, cursor, caughtUpAt ?: previous?.caughtUpAt))
        return PulledPage(
            unknownResident = unknown.map { it.id },
            droppedEdits = known.map { it.id }.filterNot { it in stored },
        )
    }

    @Query("SELECT id FROM residents WHERE id IN (:ids)")
    suspend fun knownResidentIds(ids: List<String>): List<String>

    @Query("SELECT id FROM care_notes WHERE id IN (:ids)")
    suspend fun storedNoteIds(ids: List<String>): List<String>

    /**
     * Ignores notes whose id is already stored. Their [CareNoteEntity.saveOrder] stays 0: it only orders the
     * outbox, and a pulled note is never sent back.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(notes: List<CareNoteEntity>)

    @Upsert
    suspend fun upsertCursor(cursor: SyncCursorEntity)
}

/** The ids of the pulled notes that weren't stored, by reason; see [NoteFeedDao.insertPulled]. */
data class PulledPage(val unknownResident: List<String>, val droppedEdits: List<String>)
