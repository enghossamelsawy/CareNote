package com.hossam.carenote.feature.record.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Recording a new note for one resident. Serializable, so the back stack survives process death. */
@Serializable
data class RecordKey(val residentId: String) : NavKey
