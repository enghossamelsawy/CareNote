package com.hossam.carenote.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hossam.carenote.core.database.dao.CareNoteDao
import com.hossam.carenote.core.database.dao.NoteFeedDao
import com.hossam.carenote.core.database.dao.OutboxDao
import com.hossam.carenote.core.database.dao.ResidentDao
import com.hossam.carenote.core.database.model.CareNoteEntity
import com.hossam.carenote.core.database.model.OutboxEntity
import com.hossam.carenote.core.database.model.ResidentEntity
import com.hossam.carenote.core.database.model.SyncCursorEntity

/** Every schema change adds a version and a migration in [Migrations]; the exported schemas are tested. */
@Database(
    entities = [ResidentEntity::class, CareNoteEntity::class, OutboxEntity::class, SyncCursorEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(InstantConverters::class)
abstract class CareNoteDatabase : RoomDatabase() {
    abstract fun residentDao(): ResidentDao

    abstract fun careNoteDao(): CareNoteDao

    abstract fun outboxDao(): OutboxDao

    abstract fun noteFeedDao(): NoteFeedDao
}
