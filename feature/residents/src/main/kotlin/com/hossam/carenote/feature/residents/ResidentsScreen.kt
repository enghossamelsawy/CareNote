package com.hossam.carenote.feature.residents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hossam.carenote.core.designsystem.component.Avatar
import com.hossam.carenote.core.designsystem.component.StatusBadge
import com.hossam.carenote.core.designsystem.component.StatusBanner
import com.hossam.carenote.core.designsystem.component.StatusTone
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.ui.mvi.ObserveEffects

/**
 * Connects [ResidentsScreen] to its ViewModel; navigation is passed in, so this module doesn't know other screens.
 * [onOpenDebugMenu] is set only in builds that have one; without it the screen shows no debug button.
 */
@Composable
fun ResidentsRoute(
    onOpenResident: (residentId: String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDebugMenu: (() -> Unit)? = null,
    viewModel: ResidentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is ResidentsEffect.OpenResident -> onOpenResident(effect.residentId)
        }
    }
    ResidentsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        onOpenDebugMenu = onOpenDebugMenu,
    )
}

/** The residents list. Stateless: everything comes in through [state], everything goes out through [onIntent]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentsScreen(
    state: ResidentsState,
    onIntent: (ResidentsIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDebugMenu: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.residents_title)) },
                actions = {
                    if (onOpenDebugMenu != null) {
                        IconButton(onClick = onOpenDebugMenu) {
                            Icon(
                                imageVector = CareNoteIcons.Tune,
                                contentDescription = stringResource(R.string.residents_debug_menu),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.isOffline) {
                StatusBanner(text = stringResource(R.string.residents_offline), tone = StatusTone.Warning)
            }
            ResidentsContent(state = state, onIntent = onIntent, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResidentsContent(
    state: ResidentsState,
    onIntent: (ResidentsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.residents.isEmpty() -> Box(
            modifier = modifier.fillMaxSize().padding(CareNoteTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.residents_empty),
                style = CareNoteTheme.typography.bodyLarge,
                color = CareNoteTheme.colors.onSurfaceVariant,
            )
        }

        else -> LazyColumn(modifier = modifier) {
            items(state.residents, key = { it.id }) { resident ->
                ResidentRow(
                    resident = resident,
                    onClick = { onIntent(ResidentsIntent.ResidentClicked(resident.id)) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ResidentRow(resident: ResidentItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // ListItem is at least 56dp tall, above the 48dp touch target.
    ListItem(
        leadingContent = { Avatar(name = resident.name, colorKey = resident.id) },
        headlineContent = { Text(resident.name) },
        supportingContent = { Text(stringResource(R.string.residents_room, resident.room)) },
        trailingContent = {
            val pendingCount = resident.pendingCount
            if (pendingCount > 0) {
                StatusBadge(
                    text = pluralStringResource(R.plurals.residents_pending, pendingCount, pendingCount),
                    tone = StatusTone.Warning,
                )
            }
        },
        modifier = modifier.clickable(onClick = onClick),
    )
}

@PreviewLightDark
@Composable
private fun ResidentsScreenPreview() {
    CareNoteTheme {
        ResidentsScreen(
            state = ResidentsState(
                isLoading = false,
                residents = listOf(
                    ResidentItem(id = "1", name = "Anna Becker", room = "101", pendingCount = 2),
                    ResidentItem(id = "2", name = "Karl Fischer", room = "102", pendingCount = 0),
                    ResidentItem(id = "3", name = "Greta Hoffmann", room = "103", pendingCount = 1),
                ),
            ),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ResidentsScreenOfflinePreview() {
    CareNoteTheme {
        ResidentsScreen(
            state = ResidentsState(
                isLoading = false,
                residents = listOf(ResidentItem(id = "1", name = "Anna Becker", room = "101", pendingCount = 2)),
                isOffline = true,
            ),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ResidentsScreenLoadingPreview() {
    CareNoteTheme {
        ResidentsScreen(state = ResidentsState(isLoading = true), onIntent = {})
    }
}

@PreviewLightDark
@Composable
private fun ResidentsScreenEmptyPreview() {
    CareNoteTheme {
        ResidentsScreen(state = ResidentsState(isLoading = false), onIntent = {})
    }
}
