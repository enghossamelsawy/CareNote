package com.hossam.carenote.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hossam.carenote.core.model.SyncStatus
import java.time.Instant

/**
 * A saved note. Rows are only inserted, never edited, apart from [syncStatus]: a correction is a
 * new row whose [revisionOf] points at the previous one. Ids are UUID strings, mapped in core:data.
 *
 * [saveOrder] numbers the notes 1, 2, 3… in the order they were saved on this phone, set by
 * [com.hossam.carenote.core.database.dao.CareNoteDao.insertAndQueue]. The sync queue follows it instead of a
 * clock time: an edit can only be saved after its original, so it always comes after it, even if the phone's
 * clock was moved back in between. It stays in the database; the app's model doesn't need it.
 */
@Entity(
    tableName = "care_notes",
    foreignKeys = [
        ForeignKey(
            entity = ResidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["residentId"],
        ),
    ],
    indices = [Index("residentId")],
)
data class CareNoteEntity(
    @PrimaryKey val id: String,
    val residentId: String,
    val transcript: String,
    @Embedded val vitals: VitalsEntity,
    val createdAt: Instant,
    val revisionOf: String?,
    val syncStatus: SyncStatus,
    @ColumnInfo(defaultValue = "0") val saveOrder: Long = 0,
)
