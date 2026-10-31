package com.hossam.carenote.core.model

import java.time.Instant
import java.util.UUID

/**
 * A dictated note about a resident. Notes are append-only: editing one creates a new note
 * whose [revisionOf] points at the previous version.
 */
data class CareNote(
    /** Created on the device, so retries send the same id and the server stores the note once. */
    val id: UUID,
    val residentId: String,
    val transcript: String,
    val vitals: Vitals,
    val createdAt: Instant,
    val revisionOf: UUID? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)
