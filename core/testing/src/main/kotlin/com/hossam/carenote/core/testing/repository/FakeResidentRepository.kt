package com.hossam.carenote.core.testing.repository

import com.hossam.carenote.core.domain.repository.RefreshResult
import com.hossam.carenote.core.domain.repository.ResidentRepository
import com.hossam.carenote.core.model.Resident
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [ResidentRepository] with the same sort order as the real one. */
class FakeResidentRepository : ResidentRepository {
    private val residents = MutableStateFlow<List<Resident>>(emptyList())

    /** What the next [refresh] answers; set it to simulate being offline. */
    var refreshResult = RefreshResult.Updated

    var refreshCount = 0
        private set

    /** While set and not completed, [refresh] waits on it, like a download still running. */
    var refreshGate: CompletableDeferred<Unit>? = null

    fun setResidents(value: List<Resident>) {
        residents.value = value
    }

    override suspend fun refresh(): RefreshResult {
        refreshCount++
        refreshGate?.await()
        return refreshResult
    }

    override fun observeResidents(): Flow<List<Resident>> =
        residents.map { list -> list.sortedWith(compareBy(Resident::lastName, Resident::firstName)) }

    override fun observeResident(id: String): Flow<Resident?> = residents.map { list -> list.find { it.id == id } }
}
