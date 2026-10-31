package com.hossam.carenote.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hossam.carenote.core.database.dao.CareNoteDao
import com.hossam.carenote.core.database.dao.OutboxDao
import com.hossam.carenote.core.database.dao.ResidentDao
import com.hossam.carenote.core.database.model.CareNoteEntity
import com.hossam.carenote.core.database.model.OutboxEntity
import com.hossam.carenote.core.database.model.ResidentEntity

@Database(
    entities = [ResidentEntity::class, CareNoteEntity::class, OutboxEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(InstantConverters::class)
abstract class CareNoteDatabase : RoomDatabase() {
    abstract fun residentDao(): ResidentDao

    abstract fun careNoteDao(): CareNoteDao

    abstract fun outboxDao(): OutboxDao
}
