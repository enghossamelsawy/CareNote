package com.hossam.carenote.feature.record.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hossam.carenote.feature.record.RecordRoute

/** Adds the record screen to the app's nav graph; where back and a saved note lead is decided by the caller. */
fun EntryProviderScope<NavKey>.recordEntry(onBack: () -> Unit, onNoteSave: () -> Unit) {
    entry<RecordKey> { key ->
        RecordRoute(residentId = key.residentId, onBack = onBack, onNoteSave = onNoteSave)
    }
}
