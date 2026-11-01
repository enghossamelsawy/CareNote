package com.hossam.carenote.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hossam.carenote.feature.debug.navigation.DebugKey
import com.hossam.carenote.feature.debug.navigation.debugEntry

/** Mock flavor: the debug menu exists and opens from the residents list. */
internal val debugMenuKey: NavKey? = DebugKey

internal fun EntryProviderScope<NavKey>.environmentEntries(onBack: () -> Unit) {
    debugEntry(onBack = onBack)
}
