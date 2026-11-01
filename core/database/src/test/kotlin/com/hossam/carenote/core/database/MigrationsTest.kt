package com.hossam.carenote.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Builds a database from an exported schema in `schemas/`, migrates it, and checks the result against the
 * schema Room expects, so a migration that doesn't match the entities fails here instead of on a phone.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationsTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), CareNoteDatabase::class.java)

    @Test
    fun `given notes in a v1 database, when migrating to v2, then they are numbered by when they were written`() {
        // Given
        helper.createDatabase(DATABASE, 1).use { db ->
            db.execSQL("INSERT INTO residents (id, firstName, lastName, room) VALUES ('r1', 'Anna', 'Becker', '101')")
            // Inserted out of order; "b" and "c" were written in the same second.
            insertNote(db, id = "c", createdAt = 2_000)
            insertNote(db, id = "a", createdAt = 1_000)
            insertNote(db, id = "b", createdAt = 2_000)
        }

        // When
        val migrated = helper.runMigrationsAndValidate(DATABASE, 2, true, Migrations.MIGRATION_1_2)

        // Then
        val order = migrated.query("SELECT id, saveOrder FROM care_notes ORDER BY saveOrder").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getLong(1)) }
        }
        assertEquals(listOf("a" to 1L, "c" to 2L, "b" to 3L), order)
        migrated.close()
    }

    private fun insertNote(db: SupportSQLiteDatabase, id: String, createdAt: Long) {
        db.execSQL(
            "INSERT INTO care_notes (id, residentId, transcript, createdAt, revisionOf, syncStatus, pulse) " +
                "VALUES ('$id', 'r1', 'Pulse 72', $createdAt, NULL, 'PENDING', 72)",
        )
    }

    private companion object {
        const val DATABASE = "migration-test"
    }
}
