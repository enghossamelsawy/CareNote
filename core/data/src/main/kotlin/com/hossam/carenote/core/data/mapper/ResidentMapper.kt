package com.hossam.carenote.core.data.mapper

import com.hossam.carenote.core.database.model.ResidentEntity
import com.hossam.carenote.core.model.Resident

internal fun ResidentEntity.asExternalModel() =
    Resident(id = id, firstName = firstName, lastName = lastName, room = room)

internal fun Resident.asEntity() = ResidentEntity(id = id, firstName = firstName, lastName = lastName, room = room)
