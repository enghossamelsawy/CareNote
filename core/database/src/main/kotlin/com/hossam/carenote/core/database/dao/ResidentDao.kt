package com.hossam.carenote.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hossam.carenote.core.database.model.ResidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResidentDao {
    @Query("SELECT * FROM residents ORDER BY lastName, firstName")
    fun observeAll(): Flow<List<ResidentEntity>>

    @Query("SELECT * FROM residents WHERE id = :id")
    fun observeById(id: String): Flow<ResidentEntity?>

    /** Inserts new residents and replaces existing ones with the same id. */
    @Upsert
    suspend fun upsertAll(residents: List<ResidentEntity>)
}
