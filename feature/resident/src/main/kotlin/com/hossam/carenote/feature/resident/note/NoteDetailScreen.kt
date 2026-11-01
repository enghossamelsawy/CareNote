package com.hossam.carenote.feature.resident.note

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hossam.carenote.core.designsystem.component.EmptyState
import com.hossam.carenote.core.designsystem.component.EmptyStateIcon
import com.hossam.carenote.core.designsystem.component.StatusBadge
import com.hossam.carenote.core.designsystem.component.StatusTone
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.domain.vitals.VitalKind
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.ui.mvi.ObserveEffects
import com.hossam.carenote.feature.resident.NoteHeader
import com.hossam.carenote.feature.resident.NoteItem
import com.hossam.carenote.feature.resident.vitalLabel
import com.hossam.carenote.feature.resident.vitalText
import com.hossam.carenote.feature.resident.R
import com.hossam.carenote.feature.resident.VitalTiles
import com.hossam.carenote.feature.resident.rememberNoteDateTimeFormatter
import java.time.Instant
import java.util.UUID

/** Connects [NoteDetailScreen] to a ViewModel created for the note; navigation is passed in. */
@Composable
fun NoteDetailRoute(
    residentId: String,
    noteId: UUID,
    onBack: () -> Unit,
    onEdit: (noteId: UUID) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteDetailViewModel = hiltViewModel<NoteDetailViewModel, NoteDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(residentId, noteId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            NoteDetailEffect.NavigateBack -> onBack()
            is NoteDetailEffect.OpenEditor -> onEdit(effect.noteId)
        }
    }
    NoteDetailScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** One note in full. Stateless: everything comes in through [state], everything goes out through [onIntent]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(state: NoteDetailState, onIntent: (NoteDetailIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.note_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(NoteDetailIntent.BackClicked) }) {
                        Icon(
                            imageVector = CareNoteIcons.ArrowBack,
                            contentDescription = stringResource(R.string.note_back),
                        )
                    }
                },
                actions = {
                    if (state.note != null) {
                        IconButton(onClick = { onIntent(NoteDetailIntent.EditClicked) }) {
                            Icon(
                                imageVector = CareNoteIcons.Edit,
                                contentDescription = stringResource(R.string.note_edit),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        val note = state.note
        when {
            state.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            note == null -> EmptyState(
                title = stringResource(R.string.note_not_found_title),
                message = stringResource(R.string.note_not_found_message),
                illustration = { EmptyStateIcon(CareNoteIcons.Notes) },
                modifier = contentModifier,
                actionLabel = stringResource(R.string.note_not_found_action),
                onAction = { onIntent(NoteDetailIntent.BackClicked) },
            )

            else -> NoteContent(note = note, earlier = state.earlier, onIntent = onIntent, modifier = contentModifier)
        }
    }
}

/** Scrolls as one page, so a long note reads like a document; the words can be selected and copied. */
@Composable
private fun NoteContent(
    note: NoteItem,
    earlier: List<EarlierVersion>,
    onIntent: (NoteDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(CareNoteTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.md),
    ) {
        NoteHeader(note)
        if (note.status == SyncStatus.FAILED) {
            TextButton(onClick = { onIntent(NoteDetailIntent.RetryClicked(note.id)) }) {
                Text(stringResource(R.string.resident_note_retry))
            }
        }
        VitalTiles(vitals = note.vitals, assessment = note.assessment)
        if (note.transcript.isBlank()) {
            Text(
                text = stringResource(R.string.note_no_words),
                style = CareNoteTheme.typography.bodyLarge,
                color = CareNoteTheme.colors.onSurfaceVariant,
            )
        } else {
            SelectionContainer {
                Text(text = note.transcript, style = CareNoteTheme.typography.bodyLarge)
            }
        }
        if (earlier.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.note_earlier_versions, earlier.size),
                style = CareNoteTheme.typography.titleSmall,
            )
            earlier.forEach { version ->
                EarlierVersionCard(version, onRetry = { onIntent(NoteDetailIntent.RetryClicked(version.id)) })
            }
        }
    }
}

/**
 * Muted, so it reads as history next to the current version above. Only a refused version shows its status:
 * it's the one that needs something done. The vitals the next edit changed are listed with their old values.
 */
@Composable
private fun EarlierVersionCard(version: EarlierVersion, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val formatter = rememberNoteDateTimeFormatter()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CareNoteTheme.colors.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(CareNoteTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.xs),
        ) {
            Text(
                text = formatter.format(version.writtenAt),
                style = CareNoteTheme.typography.labelMedium,
                color = CareNoteTheme.colors.onSurfaceVariant,
            )
            if (version.status == SyncStatus.FAILED) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
                ) {
                    StatusBadge(text = stringResource(R.string.resident_note_failed), tone = StatusTone.Error)
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.resident_note_retry))
                    }
                }
            }
            VitalKind.entries.filter { it in version.changed }.forEach { kind ->
                Text(
                    text = vitalChange(kind, version),
                    style = CareNoteTheme.typography.bodyMedium,
                    color = CareNoteTheme.colors.onSurfaceVariant,
                )
            }
            SelectionContainer {
                Text(
                    text = version.transcript.ifBlank { stringResource(R.string.note_no_words) },
                    style = CareNoteTheme.typography.bodyMedium,
                    color = CareNoteTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Pulse was 72 bpm", or "Temperature wasn't recorded" for a value only the edit added. */
@Composable
private fun vitalChange(kind: VitalKind, version: EarlierVersion): String {
    val text = vitalText(kind, version.vitals)
    return if (text == null) {
        stringResource(R.string.note_vital_not_recorded, vitalLabel(kind))
    } else {
        stringResource(R.string.note_vital_was, text.label, text.value, text.unit)
    }
}

@PreviewLightDark
@Composable
private fun NoteDetailScreenPreview() {
    val vitals = Vitals(temperatureCelsius = 37.0, pulse = 75)
    CareNoteTheme {
        NoteDetailScreen(
            state = NoteDetailState(
                isLoading = false,
                note = NoteItem(
                    id = UUID(0, 2),
                    createdAt = Instant.parse("2026-11-01T05:29:00Z"),
                    transcript = "Temperature is 37 and pulse is 75. Slept well, ate a full breakfast and " +
                        "walked to the garden with help. No pain reported.",
                    vitals = vitals,
                    assessment = VitalsAssessor().assess(vitals),
                    status = SyncStatus.SYNCED,
                    editedAt = Instant.parse("2026-11-01T10:15:00Z"),
                ),
                earlier = listOf(
                    EarlierVersion(
                        id = UUID(0, 1),
                        writtenAt = Instant.parse("2026-11-01T05:29:00Z"),
                        transcript = "Temperature is 38.5 and pulse is 75.",
                        vitals = Vitals(temperatureCelsius = 38.5, pulse = 75),
                        changed = setOf(VitalKind.Temperature),
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun NoteDetailScreenNotFoundPreview() {
    CareNoteTheme {
        NoteDetailScreen(state = NoteDetailState(isLoading = false, notFound = true), onIntent = {})
    }
}
