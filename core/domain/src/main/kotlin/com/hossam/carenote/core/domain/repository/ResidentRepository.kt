package com.hossam.carenote.core.domain.repository

import com.hossam.carenote.core.model.Resident
import kotlinx.coroutines.flow.Flow

/** Residents of the care home, read from local storage. */
interface ResidentRepository {
    /** Sorted by last name, then first name. */
    fun observeResidents(): Flow<List<Resident>>

    /** Emits null when no resident has this [id]. */
    fun observeResident(id: String): Flow<Resident?>
}
