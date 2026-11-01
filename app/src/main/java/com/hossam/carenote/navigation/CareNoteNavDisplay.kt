package com.hossam.carenote.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hossam.carenote.feature.record.navigation.RecordKey
import com.hossam.carenote.feature.record.navigation.recordEntry
import com.hossam.carenote.feature.resident.navigation.NoteKey
import com.hossam.carenote.feature.resident.navigation.ResidentKey
import com.hossam.carenote.feature.resident.navigation.noteEntry
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
    // Every move goes through here, so a double tap can't open a screen twice.
    val navigator = remember(backStack) { Navigator(backStack) }
    // Per flavor: the debug menu's key in mock, null in prod.
    val debugKey = debugMenuKey

    NavDisplay(
        backStack = backStack,
        onBack = navigator::goBack,
        modifier = modifier,
        // Each entry keeps its own saveable state and ViewModels, cleared when it leaves the back stack.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            residentsEntry(
                onOpenResident = { residentId ->
                    navigator.navigate(from = ResidentsKey, to = ResidentKey(residentId))
                },
                onOpenDebugMenu = debugKey?.let { key -> { navigator.navigate(from = ResidentsKey, to = key) } },
            )
            residentEntry(
                onBack = navigator::goBack,
                onRecord = { residentId ->
                    navigator.navigate(from = ResidentKey(residentId), to = RecordKey(residentId))
                },
                onOpenNote = { residentId, noteId ->
                    navigator.navigate(from = ResidentKey(residentId), to = NoteKey(residentId, noteId.toString()))
                },
            )
            noteEntry(
                onBack = navigator::goBack,
                // Back from the editor lands on this note again, now showing the edit.
                onEditNote = { from, noteId ->
                    navigator.navigate(from = from, to = RecordKey(from.residentId, noteId.toString()))
                },
            )
            recordEntry(
                onBack = navigator::goBack,
                // Back to the resident; the residents list already counts the new note as pending.
                onNoteSave = navigator::goBack,
            )
            // The debug menu in the mock flavor; nothing in prod.
            environmentEntries(onBack = navigator::goBack)
        },
    )
}
