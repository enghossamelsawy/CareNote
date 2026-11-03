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

    /**
     * Allows only one edit per version, see [com.hossam.carenote.core.database.model.CareNoteEntity]. Before
     * this, the app checked that rule in code, so a v2 database has no two edits of one version; if one did,
     * creating the index fails instead of deleting a care record to make it fit.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_care_notes_revisionOf ON care_notes (revisionOf)")
        }
    }

    /**
     * Adds [com.hossam.carenote.core.database.model.SyncCursorEntity], for pulling notes from the server. Only a
     * new table: notes and the outbox stay as they are, and the first pull starts from the server's beginning.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `sync_cursors` (`feed` TEXT NOT NULL, `cursor` TEXT NOT NULL, " +
                    "`caughtUpAt` INTEGER, PRIMARY KEY(`feed`))",
            )
        }
    }

    val all: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
