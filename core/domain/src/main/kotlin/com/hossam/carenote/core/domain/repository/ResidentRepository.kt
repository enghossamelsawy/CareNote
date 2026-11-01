package com.hossam.carenote.core.domain.repository

import com.hossam.carenote.core.model.Resident
import kotlinx.coroutines.flow.Flow

/** Residents of the care home, read from local storage. */
interface ResidentRepository {
    /** Sorted by last name, then first name. */
    fun observeResidents(): Flow<List<Resident>>

    /** Emits null when no resident has this [id]. */
    fun observeResident(id: String): Flow<Resident?>

    /** Fetches the residents from the server into local storage; the observe functions emit the result. */
    suspend fun refresh(): RefreshResult
}

enum class RefreshResult {
    Updated,

    /** Offline or the server is down: the stored residents are still shown, try again later. */
    Unavailable,

    /** The server's answer can't be used; trying again won't help. */
    Failed,
}
