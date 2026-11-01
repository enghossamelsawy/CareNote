package com.hossam.carenote.core.data.repository

import com.hossam.carenote.core.data.FakeRemoteDataSource
import com.hossam.carenote.core.data.inMemoryDatabase
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.model.ResidentEntity
import com.hossam.carenote.core.domain.repository.RefreshResult
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.network.FetchResult
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
    private val remoteDataSource = FakeRemoteDataSource()

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        repository = OfflineFirstResidentRepository(database.residentDao(), remoteDataSource)
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

    @Test
    fun `given new and changed residents on the server, when refreshing, then they are stored`() = runTest {
        // Given
        remoteDataSource.residentsResult = FetchResult.Success(
            listOf(
                Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "201"),
                Resident(id = "res-3", firstName = "Otto", lastName = "Neumann", room = "103"),
            ),
        )

        // When
        val result = repository.refresh()

        // Then
        assertEquals(RefreshResult.Updated, result)
        assertEquals(
            listOf(
                Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "201"),
                Resident(id = "res-3", firstName = "Otto", lastName = "Neumann", room = "103"),
                // Not on the server any more, but kept: their notes may still be waiting to sync.
                Resident(id = "res-2", firstName = "Max", lastName = "Weber", room = "102"),
            ),
            repository.observeResidents().first(),
        )
    }

    @Test
    fun `given the server is unreachable, when refreshing, then the stored residents stay`() = runTest {
        // Given
        remoteDataSource.residentsResult = FetchResult.Unavailable

        // When
        val result = repository.refresh()

        // Then
        assertEquals(RefreshResult.Unavailable, result)
        assertEquals(listOf("res-1", "res-2"), repository.observeResidents().first().map { it.id })
    }
}
