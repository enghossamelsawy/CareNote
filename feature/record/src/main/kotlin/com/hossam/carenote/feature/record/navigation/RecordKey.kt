package com.hossam.carenote.feature.record.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Recording a new note for one resident, or editing [noteId] when it's set.
 * Serializable, so the back stack survives process death; the id is a UUID string for that reason.
 */
@Serializable
data class RecordKey(val residentId: String, val noteId: String? = null) : NavKey
