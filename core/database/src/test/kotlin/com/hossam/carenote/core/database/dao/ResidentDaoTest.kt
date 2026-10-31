package com.hossam.carenote.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.model.ResidentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResidentDaoTest {
    private lateinit var database: CareNoteDatabase
    private lateinit var dao: ResidentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CareNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.residentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given residents, when observing all, then they are sorted by last name and first name`() = runTest {
        // Given
        dao.upsertAll(
            listOf(resident("1", "Lena", "Weber"), resident("2", "Anna", "Weber"), resident("3", "Max", "Bauer")),
        )

        // When
        val residents = dao.observeAll().first()

        // Then
        assertEquals(listOf("3", "2", "1"), residents.map { it.id })
    }

    @Test
    fun `given a resident, when observing by id, then it is emitted`() = runTest {
        // Given
        val lena = resident("1", "Lena", "Weber")
        dao.upsertAll(listOf(lena))

        // When
        val resident = dao.observeById("1").first()

        // Then
        assertEquals(lena, resident)
    }

    @Test
    fun `given no resident with that id, when observing by id, then null is emitted`() = runTest {
        // Given
        dao.upsertAll(listOf(resident("1", "Lena", "Weber")))

        // When
        val resident = dao.observeById("unknown").first()

        // Then
        assertNull(resident)
    }

    @Test
    fun `given an existing resident, when upserting a changed one, then it is replaced`() = runTest {
        // Given
        dao.upsertAll(listOf(resident("1", "Lena", "Weber", room = "101")))

        // When
        dao.upsertAll(listOf(resident("1", "Lena", "Weber", room = "204")))

        // Then
        assertEquals(listOf(resident("1", "Lena", "Weber", room = "204")), dao.observeAll().first())
    }

    @Test
    fun `given residents being observed, when one is added, then a new list is emitted`() = runTest {
        // Given
        dao.upsertAll(listOf(resident("1", "Lena", "Weber")))

        dao.observeAll().test {
            assertEquals(listOf("1"), awaitItem().map { it.id })

            // When
            dao.upsertAll(listOf(resident("2", "Max", "Bauer")))

            // Then
            assertEquals(listOf("2", "1"), awaitItem().map { it.id })
        }
    }

    private fun resident(id: String, firstName: String, lastName: String, room: String = "101") =
        ResidentEntity(id = id, firstName = firstName, lastName = lastName, room = room)
}
