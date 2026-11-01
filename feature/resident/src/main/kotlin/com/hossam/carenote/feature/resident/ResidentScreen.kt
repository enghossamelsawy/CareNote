package com.hossam.carenote.feature.resident

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.ui.mvi.ObserveEffects

/** Connects [ResidentScreen] to a ViewModel created for [residentId]; navigation is passed in. */
@Composable
fun ResidentRoute(
    residentId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResidentViewModel = hiltViewModel<ResidentViewModel, ResidentViewModel.Factory>(
        creationCallback = { factory -> factory.create(residentId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            ResidentEffect.NavigateBack -> onBack()
        }
    }
    ResidentScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** One resident's detail. Stateless: everything comes in through [state], everything goes out through [onIntent]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentScreen(state: ResidentState, onIntent: (ResidentIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (!state.isLoading && !state.notFound) {
                        Column {
                            Text(text = state.name)
                            Text(
                                text = stringResource(R.string.resident_room, state.room),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ResidentIntent.BackClicked) }) {
                        Icon(
                            imageVector = CareNoteIcons.ArrowBack,
                            contentDescription = stringResource(R.string.resident_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(CareNoteTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()

                // TODO(3.5, 4.7): replace with the record button and the notes list.
                else -> Text(
                    text = stringResource(
                        if (state.notFound) R.string.resident_not_found else R.string.resident_no_notes,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ResidentScreenPreview() {
    CareNoteTheme {
        ResidentScreen(
            state = ResidentState(isLoading = false, name = "Anna Becker", room = "101"),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ResidentScreenLoadingPreview() {
    CareNoteTheme {
        ResidentScreen(state = ResidentState(isLoading = true), onIntent = {})
    }
}

@PreviewLightDark
@Composable
private fun ResidentScreenNotFoundPreview() {
    CareNoteTheme {
        ResidentScreen(state = ResidentState(isLoading = false, notFound = true), onIntent = {})
    }
}
