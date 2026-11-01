package com.hossam.carenote.feature.resident.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hossam.carenote.feature.resident.ResidentRoute
import java.util.UUID

/**
 * Adds the resident detail screen to the app's nav graph; where back, recording and editing lead
 * is decided by the caller.
 */
fun EntryProviderScope<NavKey>.residentEntry(
    onBack: () -> Unit,
    onRecord: (residentId: String) -> Unit,
    onEditNote: (residentId: String, noteId: UUID) -> Unit,
) {
    entry<ResidentKey> { key ->
        ResidentRoute(residentId = key.residentId, onBack = onBack, onRecord = onRecord, onEditNote = onEditNote)
    }
}
