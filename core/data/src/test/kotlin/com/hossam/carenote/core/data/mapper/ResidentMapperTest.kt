package com.hossam.carenote.core.data.mapper

import com.hossam.carenote.core.database.model.ResidentEntity
import com.hossam.carenote.core.model.Resident
import org.junit.Assert.assertEquals
import org.junit.Test

class ResidentMapperTest {
    @Test
    fun `given an entity, when mapping to the model, then every field is kept`() {
        // Given
        val entity = ResidentEntity(id = "res-101", firstName = "Anna", lastName = "Becker", room = "101")

        // When
        val resident = entity.asExternalModel()

        // Then
        assertEquals(Resident(id = "res-101", firstName = "Anna", lastName = "Becker", room = "101"), resident)
    }

    @Test
    fun `given a model, when mapping to an entity and back, then it is unchanged`() {
        // Given
        val resident = Resident(id = "res-101", firstName = "Anna", lastName = "Becker", room = "101")

        // When
        val roundTrip = resident.asEntity().asExternalModel()

        // Then
        assertEquals(resident, roundTrip)
    }
}
