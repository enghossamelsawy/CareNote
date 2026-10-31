package com.hossam.carenote.core.data.repository

import com.hossam.carenote.core.data.inMemoryDatabase
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.model.ResidentEntity
import com.hossam.carenote.core.model.Resident
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
class OfflineFirstResidentRepositoryTest {
    private lateinit var database: CareNoteDatabase
    private lateinit var repository: OfflineFirstResidentRepository

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        repository = OfflineFirstResidentRepository(database.residentDao())
        database.residentDao().upsertAll(
            listOf(
                ResidentEntity(id = "res-2", firstName = "Max", lastName = "Weber", room = "102"),
                ResidentEntity(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101"),
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given stored residents, when observing all, then they come as models sorted by last name`() = runTest {
        // Given: two residents from setUp

        // When
        val residents = repository.observeResidents().first()

        // Then
        assertEquals(
            listOf(
                Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101"),
                Resident(id = "res-2", firstName = "Max", lastName = "Weber", room = "102"),
            ),
            residents,
        )
    }

    @Test
    fun `given an unknown id, when observing a resident, then null is emitted`() = runTest {
        // Given: two residents from setUp

        // When
        val resident = repository.observeResident("unknown").first()

        // Then
        assertNull(resident)
    }
}
