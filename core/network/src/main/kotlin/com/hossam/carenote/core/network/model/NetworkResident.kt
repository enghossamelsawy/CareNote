package com.hossam.carenote.core.network.model

import com.hossam.carenote.core.model.Resident
import kotlinx.serialization.Serializable

@Serializable
data class NetworkResident(val id: String, val firstName: String, val lastName: String, val room: String)

internal fun NetworkResident.asExternalModel() =
    Resident(id = id, firstName = firstName, lastName = lastName, room = room)
