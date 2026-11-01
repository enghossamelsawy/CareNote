package com.hossam.carenote.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hossam.carenote.feature.record.navigation.RecordKey
import com.hossam.carenote.feature.record.navigation.recordEntry
import com.hossam.carenote.feature.resident.navigation.ResidentKey
import com.hossam.carenote.feature.resident.navigation.residentEntry
import com.hossam.carenote.feature.residents.navigation.ResidentsKey
import com.hossam.carenote.feature.residents.navigation.residentsEntry

/**
 * The app's only navigation host. Features expose a key and an entry; this is the one place that
 * connects them, so no feature depends on another.
 */
@Composable
fun CareNoteNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(ResidentsKey)
    // Per flavor: the debug menu's key in mock, null in prod.
    val debugKey = debugMenuKey

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        // Each entry keeps its own saveable state and ViewModels, cleared when it leaves the back stack.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            residentsEntry(
                onOpenResident = { residentId -> backStack.add(ResidentKey(residentId)) },
                onOpenDebugMenu = if (debugKey == null) null else ({ backStack.add(debugKey) }),
            )
            residentEntry(
                onBack = { backStack.removeLastOrNull() },
                onRecord = { residentId -> backStack.add(RecordKey(residentId)) },
            )
            recordEntry(
                onBack = { backStack.removeLastOrNull() },
                // Back to the resident; the residents list already counts the new note as pending.
                onNoteSave = { backStack.removeLastOrNull() },
            )
            // The debug menu in the mock flavor; nothing in prod.
            environmentEntries(onBack = { backStack.removeLastOrNull() })
        },
    )
}
