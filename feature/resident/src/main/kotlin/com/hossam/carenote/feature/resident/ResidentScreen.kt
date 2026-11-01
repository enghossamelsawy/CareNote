package com.hossam.carenote.feature.resident

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hossam.carenote.core.designsystem.component.Avatar
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.ui.mvi.ObserveEffects
import java.time.Instant
import java.util.UUID

/** Connects [ResidentScreen] to a ViewModel created for [residentId]; navigation is passed in. */
@Composable
fun ResidentRoute(
    residentId: String,
    onBack: () -> Unit,
    onRecord: (residentId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResidentViewModel = hiltViewModel<ResidentViewModel, ResidentViewModel.Factory>(
        creationCallback = { factory -> factory.create(residentId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            ResidentEffect.NavigateBack -> onBack()
            is ResidentEffect.OpenRecord -> onRecord(effect.residentId)
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(name = state.name, colorKey = state.residentId)
                            Column {
                                Text(text = state.name)
                                Text(
                                    text = stringResource(R.string.resident_room, state.room),
                                    style = CareNoteTheme.typography.bodyMedium,
                                    color = CareNoteTheme.colors.onSurfaceVariant,
                                )
                            }
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
        floatingActionButton = {
            if (!state.isLoading && !state.notFound) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.resident_record_note)) },
                    icon = { Icon(imageVector = CareNoteIcons.Mic, contentDescription = null) },
                    onClick = { onIntent(ResidentIntent.RecordClicked) },
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when {
            state.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.notFound || state.notes.isEmpty() -> Box(
                modifier = contentModifier.padding(CareNoteTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (state.notFound) R.string.resident_not_found else R.string.resident_no_notes,
                    ),
                    style = CareNoteTheme.typography.bodyLarge,
                    color = CareNoteTheme.colors.onSurfaceVariant,
                )
            }

            else -> NotesList(
                notes = state.notes,
                onRetry = { noteId -> onIntent(ResidentIntent.RetryClicked(noteId)) },
                modifier = contentModifier,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ResidentScreenPreview() {
    CareNoteTheme {
        ResidentScreen(
            state = ResidentState(
                isLoading = false,
                residentId = "1",
                name = "Anna Becker",
                room = "101",
                notes = listOf(
                    previewNote(
                        id = 3,
                        status = SyncStatus.FAILED,
                        transcript = "Blood pressure 160 over 95, temperature 38.4",
                        vitals = Vitals(systolic = 160, diastolic = 95, temperatureCelsius = 38.4),
                    ),
                    previewNote(id = 2, status = SyncStatus.PENDING, transcript = "Pulse 72, temperature 36.8"),
                    previewNote(id = 1, status = SyncStatus.SYNCED, transcript = "Blood pressure 120 over 80"),
                ),
            ),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ResidentScreenNoNotesPreview() {
    CareNoteTheme {
        ResidentScreen(
            state = ResidentState(isLoading = false, residentId = "1", name = "Anna Becker", room = "101"),
            onIntent = {},
        )
    }
}

private fun previewNote(
    id: Long,
    status: SyncStatus,
    transcript: String,
    vitals: Vitals = Vitals(pulse = 72, temperatureCelsius = 36.8),
) = NoteItem(
    id = UUID(0, id),
    createdAt = Instant.parse("2026-11-01T08:30:00Z").plusSeconds(id * 3_600),
    transcript = transcript,
    vitals = vitals,
    assessment = VitalsAssessor().assess(vitals),
    status = status,
)

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
