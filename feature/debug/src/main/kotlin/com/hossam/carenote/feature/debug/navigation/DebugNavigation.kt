package com.hossam.carenote.feature.debug.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hossam.carenote.feature.debug.DebugRoute
import kotlinx.serialization.Serializable

@Serializable
data object DebugKey : NavKey

/** Adds the debug menu to the app's nav graph; only the mock flavor calls this. */
fun EntryProviderScope<NavKey>.debugEntry(onBack: () -> Unit) {
    entry<DebugKey> {
        DebugRoute(onBack = onBack)
    }
}
