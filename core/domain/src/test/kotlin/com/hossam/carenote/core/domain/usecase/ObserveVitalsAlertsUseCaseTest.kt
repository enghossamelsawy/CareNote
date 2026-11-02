package com.hossam.carenote.core.domain.usecase

import app.cash.turbine.test
import com.hossam.carenote.core.domain.vitals.VitalLevel
import com.hossam.carenote.core.domain.vitals.VitalsAlert
import com.hossam.carenote.core.domain.vitals.VitalsAssessment
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ObserveVitalsAlertsUseCaseTest {
    private val careNoteRepository = FakeCareNoteRepository()
    private val observeVitalsAlerts = ObserveVitalsAlertsUseCase(careNoteRepository, VitalsAssessor())

    private val fever = Vitals(temperatureCelsius = 38.5)
    private val inRange = Vitals(temperatureCelsius = 36.8)
    private val feverAlert = VitalsAssessment(temperature = VitalLevel.High)

    @Test
    fun `given a fever in the latest note, when observing, then the resident has an alert from that note`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = 10, vitals = fever))

        // When
        val alerts = observeVitalsAlerts().first()

        // Then
        assertEquals(mapOf(ANNA to VitalsAlert(feverAlert, takenAt = Instant.ofEpochSecond(10))), alerts)
    }

    @Test
    fun `given a fever, when a newer reading is in range, then the alert clears`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = 10, vitals = fever))

        observeVitalsAlerts().test {
            assertEquals(setOf(ANNA), awaitItem().keys)

            // When
            careNoteRepository.save(note(id = 2, writtenAt = 20, vitals = inRange))

            // Then
            assertEquals(emptyMap<String, VitalsAlert>(), awaitItem())
        }
    }

    @Test
    fun `given a fever, when a newer note has no vitals, then the alert stays`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = 10, vitals = fever))
        careNoteRepository.save(note(id = 2, writtenAt = 20, vitals = Vitals.EMPTY))

        // When
        val alerts = observeVitalsAlerts().first()

        // Then
        assertEquals(mapOf(ANNA to VitalsAlert(feverAlert, takenAt = Instant.ofEpochSecond(10))), alerts)
    }

    @Test
    fun `given a fever, when an edit corrects it to a value in range, then there is no alert`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = 10, vitals = fever))
        careNoteRepository.save(note(id = 2, writtenAt = 30, vitals = inRange, revisionOf = UUID(0, 1)))

        // When
        val alerts = observeVitalsAlerts().first()

        // Then
        assertEquals(emptyMap<String, VitalsAlert>(), alerts)
    }

    @Test
    fun `given a newer fever, when an older note is edited later, then the fever still counts as latest`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = 10, vitals = inRange))
        careNoteRepository.save(note(id = 2, writtenAt = 20, vitals = fever))

        // When
        careNoteRepository.save(note(id = 3, writtenAt = 30, vitals = inRange, revisionOf = UUID(0, 1)))

        // Then
        assertEquals(
            mapOf(ANNA to VitalsAlert(feverAlert, takenAt = Instant.ofEpochSecond(20))),
            observeVitalsAlerts().first(),
        )
    }

    @Test
    fun `given two residents, when only one has vitals out of range, then only that one has an alert`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = 10, vitals = fever, residentId = ANNA))
        careNoteRepository.save(note(id = 2, writtenAt = 10, vitals = inRange, residentId = MAX))

        // When
        val alerts = observeVitalsAlerts().first()

        // Then
        assertEquals(setOf(ANNA), alerts.keys)
    }

    private fun note(id: Long, writtenAt: Long, vitals: Vitals, residentId: String = ANNA, revisionOf: UUID? = null) =
        CareNote(
            id = UUID(0, id),
            residentId = residentId,
            transcript = "Temperature",
            vitals = vitals,
            createdAt = Instant.ofEpochSecond(writtenAt),
            revisionOf = revisionOf,
        )

    private companion object {
        const val ANNA = "res-1"
        const val MAX = "res-2"
    }
}
