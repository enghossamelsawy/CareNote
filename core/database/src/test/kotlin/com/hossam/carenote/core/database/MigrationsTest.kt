package com.hossam.carenote.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Builds a database from an exported schema in `schemas/`, then opens it the way the app does, with every
 * migration. Room checks the migrated tables against the entities on open, so a migration that doesn't match
 * them fails here instead of on a phone.
 *
 * Room's MigrationTestHelper would do the same, but it opens databases by a path its driver rejects on
 * Windows, where these tests also run.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        context.deleteDatabase(DATABASE)
    }

    @Test
    fun `given notes in a v1 database, when migrating to v2, then they are numbered by when they were written`() {
        // Given
        createDatabase(version = 1).use { db ->
            db.execSQL("INSERT INTO residents (id, firstName, lastName, room) VALUES ('r1', 'Anna', 'Becker', '101')")
            // Inserted out of order; "b" and "c" were written in the same second.
            insertNote(db, id = "c", createdAt = 2_000)
            insertNote(db, id = "a", createdAt = 1_000)
            insertNote(db, id = "b", createdAt = 2_000)
        }

        // When
        val migrated = openWithMigrations()

        // Then
        val order = migrated.openHelper.readableDatabase
            .query("SELECT id, saveOrder FROM care_notes ORDER BY saveOrder")
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getLong(1)) } }
        assertEquals(listOf("a" to 1L, "c" to 2L, "b" to 3L), order)
        migrated.close()
    }

    /** The database exactly as [version] created it, from that version's exported schema. */
    private fun createDatabase(version: Int): SQLiteDatabase {
        val json = context.assets.open("$SCHEMAS/$version.json").bufferedReader().use { it.readText() }
        val schema = JSONObject(json).getJSONObject("database")
        val db = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace(TABLE_NAME, table))
            val indices = entity.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(j).getString("createSql").replace(TABLE_NAME, table))
            }
        }
        val setupQueries = schema.getJSONArray("setupQueries")
        for (i in 0 until setupQueries.length()) db.execSQL(setupQueries.getString(i))
        db.version = version
        return db
    }

    private fun openWithMigrations(): CareNoteDatabase = Room
        .databaseBuilder(context, CareNoteDatabase::class.java, DATABASE)
        .addMigrations(*Migrations.all)
        // Room's default WAL mode keeps a second file that SQLite can't open under Robolectric on Windows.
        // The journal mode doesn't change what a migration does; the app keeps WAL.
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
        .allowMainThreadQueries()
        .build()

    private fun insertNote(db: SQLiteDatabase, id: String, createdAt: Long) {
        db.execSQL(
            "INSERT INTO care_notes (id, residentId, transcript, createdAt, revisionOf, syncStatus, pulse) " +
                "VALUES ('$id', 'r1', 'Pulse 72', $createdAt, NULL, 'PENDING', 72)",
        )
    }

    private companion object {
        const val DATABASE = "migration-test"
        const val SCHEMAS = "com.hossam.carenote.core.database.CareNoteDatabase"
        const val TABLE_NAME = "\${TABLE_NAME}"
    }
}
