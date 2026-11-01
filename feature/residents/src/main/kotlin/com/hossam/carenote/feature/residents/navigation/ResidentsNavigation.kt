package com.hossam.carenote.feature.residents.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hossam.carenote.feature.residents.ResidentsRoute
import kotlinx.serialization.Serializable

/** The residents list. Serializable, so the back stack survives process death. */
@Serializable
data object ResidentsKey : NavKey

/** Adds the residents list to the app's nav graph; where a resident opens is decided by the caller. */
fun EntryProviderScope<NavKey>.residentsEntry(onOpenResident: (residentId: String) -> Unit) {
    entry<ResidentsKey> {
        ResidentsRoute(onOpenResident = onOpenResident)
    }
}
