package com.hossam.carenote.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hossam.carenote.core.database.model.CareNoteEntity
import com.hossam.carenote.core.database.model.OutboxEntity
import com.hossam.carenote.core.database.model.ResidentEntity
import com.hossam.carenote.core.database.model.VitalsEntity
import com.hossam.carenote.core.model.SyncStatus
import java.time.Instant

/** A fresh in-memory database; main-thread queries are allowed in tests only. */
fun inMemoryDatabase(): CareNoteDatabase = Room
    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CareNoteDatabase::class.java)
    .allowMainThreadQueries()
    .build()

fun resident(id: String, firstName: String = "Lena", lastName: String = "Weber", room: String = "101") =
    ResidentEntity(id = id, firstName = firstName, lastName = lastName, room = room)

fun careNote(
    id: String,
    residentId: String,
    createdAt: Instant = Instant.EPOCH,
    syncStatus: SyncStatus = SyncStatus.PENDING,
    revisionOf: String? = null,
) = CareNoteEntity(
    id = id,
    residentId = residentId,
    transcript = "Pulse 72",
    vitals = VitalsEntity(
        systolic = null,
        diastolic = null,
        pulse = 72,
        temperatureCelsius = null,
        oxygenSaturation = null,
    ),
    createdAt = createdAt,
    revisionOf = revisionOf,
    syncStatus = syncStatus,
)

fun outboxEntry(noteId: String, enqueuedAt: Instant = Instant.EPOCH) =
    OutboxEntity(noteId = noteId, enqueuedAt = enqueuedAt)
