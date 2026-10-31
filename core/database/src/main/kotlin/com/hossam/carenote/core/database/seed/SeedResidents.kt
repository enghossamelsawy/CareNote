package com.hossam.carenote.core.database.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hossam.carenote.core.database.model.ResidentEntity

// TODO(4.3): move to the mock flavor; in prod, residents come from GET /residents.

/** Made-up residents, so the app has something to show before it can reach the server. */
internal val seedResidents = listOf(
    ResidentEntity(id = "res-101", firstName = "Anna", lastName = "Becker", room = "101"),
    ResidentEntity(id = "res-102", firstName = "Karl", lastName = "Fischer", room = "102"),
    ResidentEntity(id = "res-103", firstName = "Greta", lastName = "Hoffmann", room = "103"),
    ResidentEntity(id = "res-104", firstName = "Hans", lastName = "Klein", room = "104"),
    ResidentEntity(id = "res-105", firstName = "Ingrid", lastName = "Lang", room = "105"),
    ResidentEntity(id = "res-106", firstName = "Otto", lastName = "Neumann", room = "106"),
    ResidentEntity(id = "res-107", firstName = "Rosa", lastName = "Schulz", room = "107"),
    ResidentEntity(id = "res-108", firstName = "Walter", lastName = "Vogel", room = "108"),
)

/**
 * Inserts [seedResidents] when the database file is first created. [onCreate] runs once, inside the
 * transaction that creates the tables, so no query can see the table empty.
 */
internal object SeedResidentsCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        seedResidents.forEach { resident ->
            db.execSQL(
                "INSERT INTO residents (id, firstName, lastName, room) VALUES (?, ?, ?, ?)",
                arrayOf(resident.id, resident.firstName, resident.lastName, resident.room),
            )
        }
    }
}
