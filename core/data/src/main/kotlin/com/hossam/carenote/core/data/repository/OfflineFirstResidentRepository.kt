package com.hossam.carenote.core.data.repository

import com.hossam.carenote.core.data.mapper.asEntity
import com.hossam.carenote.core.data.mapper.asExternalModel
import com.hossam.carenote.core.database.dao.ResidentDao
import com.hossam.carenote.core.domain.repository.RefreshResult
import com.hossam.carenote.core.domain.repository.ResidentRepository
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.network.FetchResult
import com.hossam.carenote.core.network.RemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The local database is the single source of truth for the UI; [refresh] only writes into it,
 * and the observing screens update by themselves.
 */
internal class OfflineFirstResidentRepository @Inject constructor(
    private val residentDao: ResidentDao,
    private val remoteDataSource: RemoteDataSource,
) : ResidentRepository {
    override fun observeResidents(): Flow<List<Resident>> =
        residentDao.observeAll().map { residents -> residents.map { it.asExternalModel() } }

    override fun observeResident(id: String): Flow<Resident?> =
        residentDao.observeById(id).map { it?.asExternalModel() }

    /** Adds and updates residents; never deletes one, since their notes may still be waiting to sync. */
    override suspend fun refresh(): RefreshResult = when (val result = remoteDataSource.fetchResidents()) {
        is FetchResult.Success -> {
            residentDao.upsertAll(result.value.map { it.asEntity() })
            RefreshResult.Updated
        }

        FetchResult.Unavailable -> RefreshResult.Unavailable
        FetchResult.Failed -> RefreshResult.Failed
    }
}
