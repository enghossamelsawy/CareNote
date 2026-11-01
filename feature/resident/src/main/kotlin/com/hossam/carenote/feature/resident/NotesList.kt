package com.hossam.carenote.feature.resident

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.hossam.carenote.core.designsystem.component.StatusBadge
import com.hossam.carenote.core.designsystem.component.StatusTone
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

/** Newest first; leaves room at the bottom so the record button never covers the last note. */
@Composable
internal fun NotesList(notes: List<NoteItem>, onRetry: (noteId: UUID) -> Unit, modifier: Modifier = Modifier) {
    val spacing = CareNoteTheme.spacing
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = spacing.md,
            top = spacing.md,
            end = spacing.md,
            // Past the record button, which floats over the list.
            bottom = spacing.xxl * 2,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(note = note, onRetry = { onRetry(note.id) })
        }
    }
}

@Composable
private fun NoteCard(note: NoteItem, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    // In the phone's own time zone and date format.
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault())
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(CareNoteTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatter.format(note.createdAt),
                    modifier = Modifier.weight(1f),
                    style = CareNoteTheme.typography.labelLarge,
                    color = CareNoteTheme.colors.onSurfaceVariant,
                )
                StatusBadge(text = stringResource(note.status.label()), tone = note.status.tone())
            }

            if (note.transcript.isNotBlank()) {
                Text(
                    text = note.transcript,
                    style = CareNoteTheme.typography.bodyLarge,
                    maxLines = TRANSCRIPT_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            vitalsSummary(note.vitals)?.let { summary ->
                Text(
                    text = summary,
                    style = CareNoteTheme.typography.bodyMedium,
                    color = CareNoteTheme.colors.onSurfaceVariant,
                )
            }

            if (note.status == SyncStatus.FAILED) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.resident_note_retry))
                }
            }
        }
    }
}

/** "BP 120/80 · Pulse 72 · 36.8 °C", or null when no vitals were recorded. */
@Composable
private fun vitalsSummary(vitals: Vitals): String? {
    val systolic = vitals.systolic
    val diastolic = vitals.diastolic
    val pulse = vitals.pulse
    val temperature = vitals.temperatureCelsius
    val oxygenSaturation = vitals.oxygenSaturation
    val parts = buildList {
        if (systolic != null && diastolic != null) add(stringResource(R.string.resident_vital_bp, systolic, diastolic))
        if (pulse != null) add(stringResource(R.string.resident_vital_pulse, pulse))
        if (temperature != null) add(stringResource(R.string.resident_vital_temperature, temperature.toString()))
        if (oxygenSaturation != null) add(stringResource(R.string.resident_vital_spo2, oxygenSaturation))
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(separator = " · ")
}

/** The text always says the status; the colour only adds to it. */
@StringRes
private fun SyncStatus.label(): Int = when (this) {
    SyncStatus.PENDING -> R.string.resident_note_pending
    SyncStatus.SYNCING -> R.string.resident_note_syncing
    SyncStatus.SYNCED -> R.string.resident_note_synced
    SyncStatus.FAILED -> R.string.resident_note_failed
}

private fun SyncStatus.tone(): StatusTone = when (this) {
    SyncStatus.PENDING -> StatusTone.Warning
    SyncStatus.SYNCING -> StatusTone.Neutral
    SyncStatus.SYNCED -> StatusTone.Success
    SyncStatus.FAILED -> StatusTone.Error
}

private const val TRANSCRIPT_MAX_LINES = 4
