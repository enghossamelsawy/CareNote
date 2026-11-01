package com.hossam.carenote.feature.record.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hossam.carenote.feature.record.RecordRoute

/** Adds the record screen to the app's nav graph; what back does is decided by the caller. */
fun EntryProviderScope<NavKey>.recordEntry(onBack: () -> Unit) {
    entry<RecordKey> { key ->
        RecordRoute(residentId = key.residentId, onBack = onBack)
    }
}
