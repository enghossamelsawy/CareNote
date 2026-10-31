package com.hossam.carenote.core.database.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hossam.carenote.core.database.CareNoteDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeedResidentsCallbackTest {
    @Test
    fun `given a new database with the seed callback, when observing residents, then the 8 seed residents are there`() =
        runTest {
            // Given
            val context = ApplicationProvider.getApplicationContext<Context>()
            val database = Room.inMemoryDatabaseBuilder(context, CareNoteDatabase::class.java)
                .addCallback(SeedResidentsCallback)
                .allowMainThreadQueries()
                .build()

            // When
            val residents = database.residentDao().observeAll().first()

            // Then
            assertEquals(8, residents.size)
            assertEquals(seedResidents.toSet(), residents.toSet())
            database.close()
        }
}
