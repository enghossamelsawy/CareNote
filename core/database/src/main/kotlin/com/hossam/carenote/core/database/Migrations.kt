package com.hossam.carenote.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Schema changes between versions. Notes are care records, so a migration never drops data. */
object Migrations {
    /**
     * Adds [com.hossam.carenote.core.database.model.CareNoteEntity.saveOrder] and numbers the existing notes by
     * when they were written, the best order v1 knows; notes written at the same time keep their insert order.
     * A COUNT subquery instead of ROW_NUMBER(), which needs SQLite 3.25 and Android 8 ships 3.18.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE care_notes ADD COLUMN saveOrder INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                UPDATE care_notes SET saveOrder = (
                    SELECT COUNT(*) FROM care_notes AS earlier
                    WHERE earlier.createdAt < care_notes.createdAt
                        OR (earlier.createdAt = care_notes.createdAt AND earlier.rowid <= care_notes.rowid)
                )
                """.trimIndent(),
            )
        }
    }

    val all: Array<Migration> = arrayOf(MIGRATION_1_2)
}
