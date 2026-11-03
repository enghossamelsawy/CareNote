package com.hossam.carenote.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * How far this phone has read one of the server's feeds, such as [NOTES_FEED]. [cursor] is the server's,
 * stored as it came and sent back as it is.
 *
 * [caughtUpAt] is when the phone last read the feed to its end, so it had every note the server had; null
 * until that first happens. A pull that stops halfway moves [cursor] but not [caughtUpAt].
 */
@Entity(tableName = "sync_cursors")
data class SyncCursorEntity(@PrimaryKey val feed: String, val cursor: String, val caughtUpAt: Instant?) {
    companion object {
        const val NOTES_FEED = "notes"
    }
}
