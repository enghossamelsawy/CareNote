package com.hossam.carenote.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hossam.carenote.feature.residents.navigation.ResidentsKey
import com.hossam.carenote.feature.residents.navigation.residentsEntry

/**
 * The app's only navigation host. Features expose a key and an entry; this is the one place that
 * connects them, so no feature depends on another.
 */
@Composable
fun CareNoteNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(ResidentsKey)

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
            // TODO(2.3): open the resident detail screen.
            residentsEntry(onOpenResident = {})
        },
    )
}
