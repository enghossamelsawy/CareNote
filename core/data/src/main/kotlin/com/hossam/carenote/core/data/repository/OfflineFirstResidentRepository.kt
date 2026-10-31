package com.hossam.carenote.core.data.repository

import com.hossam.carenote.core.data.mapper.asExternalModel
import com.hossam.carenote.core.database.dao.ResidentDao
import com.hossam.carenote.core.domain.repository.ResidentRepository
import com.hossam.carenote.core.model.Resident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Reads residents from the local database, which is the single source of truth for the UI. */
internal class OfflineFirstResidentRepository @Inject constructor(private val residentDao: ResidentDao) :
    ResidentRepository {
    override fun observeResidents(): Flow<List<Resident>> =
        residentDao.observeAll().map { residents -> residents.map { it.asExternalModel() } }

    override fun observeResident(id: String): Flow<Resident?> =
        residentDao.observeById(id).map { it?.asExternalModel() }
}
