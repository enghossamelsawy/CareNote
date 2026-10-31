package com.hossam.carenote.core.database.model

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
)
