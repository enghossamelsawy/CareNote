package com.hossam.carenote.feature.residents

import app.cash.turbine.test
import com.hossam.carenote.core.domain.repository.RefreshResult
import com.hossam.carenote.core.domain.usecase.ObserveNetworkStatusUseCase
import com.hossam.carenote.core.domain.usecase.ObserveOverdueResidentsUseCase
import com.hossam.carenote.core.domain.usecase.ObserveResidentsWithPendingCountUseCase
import com.hossam.carenote.core.domain.usecase.ObserveVitalsAlertsUseCase
import com.hossam.carenote.core.domain.usecase.RefreshResidentsUseCase
import com.hossam.carenote.core.domain.vitals.VitalLevel
import com.hossam.carenote.core.domain.vitals.VitalsAlert
import com.hossam.carenote.core.domain.vitals.VitalsAssessment
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.MainDispatcherRule
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeCurrentTime
import com.hossam.carenote.core.testing.repository.FakeNetworkMonitor
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import com.hossam.carenote.core.ui.mvi.MviViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ResidentsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val residentRepository = FakeResidentRepository()
    private val careNoteRepository = FakeCareNoteRepository()
    private val networkMonitor = FakeNetworkMonitor()
    private val currentTime = FakeCurrentTime()

    private val anna = Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")
    private val max = Resident(id = "res-2", firstName = "Max", lastName = "Weber", room = "102")

    @Test
    fun `given residents, when the view model starts, then they are shown as items`() = runTest {
        // Given
        residentRepository.setResidents(listOf(max, anna))
        careNoteRepository.save(note(id = 1, residentId = anna.id))

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(
            ResidentsState(
                isLoading = false,
                residents = listOf(
                    ResidentItem(id = "res-1", name = "Anna Becker", room = "101", pendingCount = 1),
                    ResidentItem(id = "res-2", name = "Max Weber", room = "102", pendingCount = 0),
                ),
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun `given a resident with a fever, when the view model starts, then only their item has the alert`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna, max))
        careNoteRepository.save(note(id = 1, residentId = anna.id, vitals = Vitals(temperatureCelsius = 38.5)))
        careNoteRepository.save(note(id = 2, residentId = max.id))

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(
            listOf(
                VitalsAlert(VitalsAssessment(temperature = VitalLevel.High), takenAt = Instant.EPOCH),
                null,
            ),
            viewModel.state.value.residents.map { it.alert },
        )
    }

    @Test
    fun `given a resident with a fever, when a newer reading is in range, then the alert goes away`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, residentId = anna.id, vitals = Vitals(temperatureCelsius = 38.5)))
        val viewModel = createViewModel()

        // When
        val inRange = Vitals(temperatureCelsius = 36.8)
        val later = Instant.ofEpochSecond(60)
        careNoteRepository.save(note(id = 2, residentId = anna.id, vitals = inRange, createdAt = later))

        // Then
        assertEquals(null, viewModel.state.value.residents.single().alert)
    }

    @Test
    fun `given a note 8 hours ago, when the view model starts, then the resident is overdue since it`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, residentId = anna.id))
        currentTime.set(Instant.EPOCH.plus(ObserveOverdueResidentsUseCase.OVERDUE_AFTER))

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(Instant.EPOCH, viewModel.state.value.residents.single().overdueSince)
    }

    @Test
    fun `given an overdue resident, when a new note is saved, then they are no longer overdue`() = runTest {
        // Given
        val now = Instant.EPOCH.plus(ObserveOverdueResidentsUseCase.OVERDUE_AFTER)
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, residentId = anna.id))
        currentTime.set(now)
        val viewModel = createViewModel()

        // When
        careNoteRepository.save(note(id = 2, residentId = anna.id, createdAt = now))

        // Then
        assertEquals(null, viewModel.state.value.residents.single().overdueSince)
    }

    @Test
    fun `given a shown resident, when their note syncs, then the pending count is updated`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, residentId = anna.id))
        val viewModel = createViewModel()

        // When
        careNoteRepository.setSyncStatus(UUID(0, 1), SyncStatus.SYNCED)

        // Then
        assertEquals(0, viewModel.state.value.residents.single().pendingCount)
    }

    @Test
    fun `given the list, when a resident is clicked, then the resident is opened`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(ResidentsIntent.ResidentClicked(anna.id))

        // Then
        viewModel.effects.test {
            assertEquals(ResidentsEffect.OpenResident(anna.id), awaitItem())
        }
    }

    @Test
    fun `given the list, when the screen opens, then residents are refreshed from the server`() = runTest {
        // When
        createViewModel()

        // Then
        assertEquals(1, residentRepository.refreshCount)
    }

    @Test
    fun `given the server is unreachable, when the screen opens, then the stored residents still show`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        residentRepository.refreshResult = RefreshResult.Unavailable

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(listOf("res-1"), viewModel.state.value.residents.map { it.id })
    }

    @Test
    fun `given the first download failed, when try again is clicked, then residents are refreshed again`() = runTest {
        // Given
        residentRepository.refreshResult = RefreshResult.Unavailable
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(ResidentsIntent.RetryClicked)

        // Then
        assertEquals(2, residentRepository.refreshCount)
    }

    @Test
    fun `given the first download is running, when the screen shows, then it says it is downloading`() = runTest {
        // Given
        residentRepository.refreshGate = CompletableDeferred()

        // When
        val viewModel = createViewModel()

        // Then
        assertTrue(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `given a download is running, when try again is clicked, then no second download starts`() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()
        residentRepository.refreshGate = gate
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(ResidentsIntent.RetryClicked)
        gate.complete(Unit)

        // Then
        assertEquals(1, residentRepository.refreshCount)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `given the server is unreachable, when the download ends, then that problem is shown`() = runTest {
        // Given
        residentRepository.refreshResult = RefreshResult.Unavailable

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(RefreshProblem.Unavailable, viewModel.state.value.refreshProblem)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `given the server answers with something unusable, when the download ends, then that problem is shown`() =
        runTest {
            // Given
            residentRepository.refreshResult = RefreshResult.Failed

            // When
            val viewModel = createViewModel()

            // Then
            assertEquals(RefreshProblem.Failed, viewModel.state.value.refreshProblem)
        }

    @Test
    fun `given a failed download, when try again works, then the problem is cleared`() = runTest {
        // Given
        residentRepository.refreshResult = RefreshResult.Unavailable
        val viewModel = createViewModel()
        residentRepository.refreshResult = RefreshResult.Updated

        // When
        viewModel.onIntent(ResidentsIntent.RetryClicked)

        // Then
        assertEquals(null, viewModel.state.value.refreshProblem)
    }

    @Test
    fun `given the list was hidden, when a note syncs meanwhile, then the count updates once it shows again`() =
        runTest {
            // Given
            residentRepository.setResidents(listOf(anna))
            careNoteRepository.save(note(id = 1, residentId = anna.id))
            val viewModel = newViewModel()
            show(viewModel).cancel()
            advanceTimeBy(MviViewModel.STOP_TIMEOUT + 1.seconds)
            careNoteRepository.setSyncStatus(UUID(0, 1), SyncStatus.SYNCED)
            assertEquals(1, viewModel.state.value.residents.single().pendingCount)

            // When
            show(viewModel)

            // Then
            assertEquals(0, viewModel.state.value.residents.single().pendingCount)
        }

    /** A view model whose screen is shown; the residents only update while it is. */
    private fun TestScope.createViewModel() = newViewModel().also { show(it) }

    /** Collects the state like the visible screen would, until the returned job is cancelled. */
    private fun TestScope.show(viewModel: ResidentsViewModel): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

    private fun newViewModel() = ResidentsViewModel(
        observeResidentsWithPendingCount = ObserveResidentsWithPendingCountUseCase(
            residentRepository = residentRepository,
            careNoteRepository = careNoteRepository,
        ),
        refreshResidents = RefreshResidentsUseCase(residentRepository),
        observeVitalsAlerts = ObserveVitalsAlertsUseCase(careNoteRepository, VitalsAssessor()),
        observeOverdueResidents = ObserveOverdueResidentsUseCase(careNoteRepository, currentTime),
        observeNetworkStatus = ObserveNetworkStatusUseCase(networkMonitor),
    )

    @Test
    fun `given the list, when the device goes offline, then the offline banner shows`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        networkMonitor.setOnline(false)

        // Then
        assertEquals(true, viewModel.state.value.isOffline)
    }

    private fun note(
        id: Int,
        residentId: String,
        vitals: Vitals = Vitals(pulse = 72),
        createdAt: Instant = Instant.EPOCH,
    ) = CareNote(
        id = UUID(0, id.toLong()),
        residentId = residentId,
        transcript = "Pulse 72",
        vitals = vitals,
        createdAt = createdAt,
    )
}
