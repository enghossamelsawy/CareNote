package com.hossam.carenote.feature.resident

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.hossam.carenote.core.designsystem.component.StatusBadge
import com.hossam.carenote.core.designsystem.component.StatusTone
import com.hossam.carenote.core.designsystem.component.VitalTile
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.domain.vitals.VitalLevel
import com.hossam.carenote.core.domain.vitals.VitalsAssessment
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

/** Newest first; leaves room at the bottom so the record button never covers the last note. */
@Composable
internal fun NotesList(
    notes: List<NoteItem>,
    onRetry: (noteId: UUID) -> Unit,
    onOpen: (noteId: UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            NoteCard(note = note, onRetry = { onRetry(note.id) }, onOpen = { onOpen(note.id) })
        }
    }
}

/**
 * A preview of one note; tapping it opens the note in full. A red edge on the left marks a note with
 * a vital out of range, so it's found while scrolling.
 */
@Composable
private fun NoteCard(note: NoteItem, onRetry: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CareNoteTheme.colors.surfaceContainerLow),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (note.assessment.needsAttention) {
                Box(
                    modifier = Modifier
                        .width(CareNoteTheme.dimens.accentStrip)
                        .fillMaxHeight()
                        .background(CareNoteTheme.statusColors.errorIndicator),
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(CareNoteTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
            ) {
                NoteHeader(note)
                if (note.earlierNotSynced) {
                    Text(
                        text = stringResource(R.string.resident_note_earlier_not_synced),
                        style = CareNoteTheme.typography.labelMedium,
                        color = CareNoteTheme.statusColors.errorIndicator,
                    )
                }
                VitalTiles(vitals = note.vitals, assessment = note.assessment)
                if (note.transcript.isNotBlank()) {
                    Text(
                        text = note.transcript,
                        style = CareNoteTheme.typography.bodyMedium,
                        color = CareNoteTheme.colors.onSurfaceVariant,
                        maxLines = TRANSCRIPT_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
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
}

/**
 * When the note was written, and when it was last edited; then the sync status: a quiet check once
 * synced, a badge only while it still needs something.
 */
@Composable
internal fun NoteHeader(note: NoteItem, modifier: Modifier = Modifier) {
    val zone = remember { ZoneId.systemDefault() }
    val dateTime = rememberNoteDateTimeFormatter()
    val time = remember(zone) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone) }
    val written = dateTime.format(note.createdAt)
    val editedAt = note.editedAt
    val header = if (editedAt == null) {
        written
    } else {
        // Only the time when edited the same day, so the header stays short.
        val sameDay = editedAt.atZone(zone).toLocalDate() == note.createdAt.atZone(zone).toLocalDate()
        val edited = if (sameDay) time.format(editedAt) else dateTime.format(editedAt)
        stringResource(R.string.resident_note_written_edited, written, edited)
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = header,
            modifier = Modifier.weight(1f),
            style = CareNoteTheme.typography.labelLarge,
            color = CareNoteTheme.colors.onSurfaceVariant,
        )
        if (note.status == SyncStatus.SYNCED) {
            Icon(
                imageVector = CareNoteIcons.Check,
                contentDescription = stringResource(note.status.label()),
                modifier = Modifier.size(CareNoteTheme.dimens.iconSm),
                tint = CareNoteTheme.statusColors.successIndicator,
            )
        } else {
            StatusBadge(text = stringResource(note.status.label()), tone = note.status.tone())
        }
    }
}

/** Date and time in the phone's own time zone and format, such as "Nov 1, 2026, 5:29 AM". */
@Composable
internal fun rememberNoteDateTimeFormatter(): DateTimeFormatter = remember {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault())
}

/** Two equal columns, so every tile has the same width; an odd last tile stays on the left. Nothing without vitals. */
@Composable
internal fun VitalTiles(vitals: Vitals, assessment: VitalsAssessment, modifier: Modifier = Modifier) {
    val tiles = vitalTiles(vitals, assessment)
    if (tiles.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm)) {
        tiles.chunked(TILES_PER_ROW).forEach { row ->
            // Tiles in a row share the tallest one's height.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
            ) {
                row.forEach { tile ->
                    VitalTile(
                        label = tile.label,
                        value = tile.value,
                        unit = tile.unit,
                        level = stringResource(tile.level.label()),
                        needsAttention = tile.level != VitalLevel.Normal,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        levelIcon = tile.level.icon(),
                    )
                }
                repeat(TILES_PER_ROW - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private class VitalTileContent(val label: String, val value: String, val unit: String, val level: VitalLevel)

/** Formatted for the reader's locale, e.g. "36,8" in German. */
@Composable
private fun vitalTiles(vitals: Vitals, assessment: VitalsAssessment): List<VitalTileContent> {
    val systolic = vitals.systolic
    val diastolic = vitals.diastolic
    val bloodPressure = assessment.bloodPressure
    val pulse = vitals.pulse
    val pulseLevel = assessment.pulse
    val temperature = vitals.temperatureCelsius
    val temperatureLevel = assessment.temperature
    val oxygenSaturation = vitals.oxygenSaturation
    val oxygenSaturationLevel = assessment.oxygenSaturation
    return buildList {
        if (systolic != null && diastolic != null && bloodPressure != null) {
            add(
                VitalTileContent(
                    label = stringResource(R.string.resident_vital_bp_label),
                    value = stringResource(R.string.resident_vital_bp_value, systolic, diastolic),
                    unit = stringResource(R.string.resident_vital_unit_mmhg),
                    level = bloodPressure,
                ),
            )
        }
        if (pulse != null && pulseLevel != null) {
            add(
                VitalTileContent(
                    label = stringResource(R.string.resident_vital_pulse_label),
                    value = stringResource(R.string.resident_vital_whole_value, pulse),
                    unit = stringResource(R.string.resident_vital_unit_bpm),
                    level = pulseLevel,
                ),
            )
        }
        if (temperature != null && temperatureLevel != null) {
            add(
                VitalTileContent(
                    label = stringResource(R.string.resident_vital_temperature_label),
                    value = stringResource(R.string.resident_vital_temperature_value, temperature),
                    unit = stringResource(R.string.resident_vital_unit_celsius),
                    level = temperatureLevel,
                ),
            )
        }
        if (oxygenSaturation != null && oxygenSaturationLevel != null) {
            add(
                VitalTileContent(
                    label = stringResource(R.string.resident_vital_spo2_label),
                    value = stringResource(R.string.resident_vital_whole_value, oxygenSaturation),
                    unit = stringResource(R.string.resident_vital_unit_percent),
                    level = oxygenSaturationLevel,
                ),
            )
        }
    }
}

@StringRes
private fun VitalLevel.label(): Int = when (this) {
    VitalLevel.Normal -> R.string.resident_vital_normal
    VitalLevel.High -> R.string.resident_vital_high
    VitalLevel.Low -> R.string.resident_vital_low
}

private fun VitalLevel.icon(): ImageVector? = when (this) {
    VitalLevel.Normal -> null
    VitalLevel.High -> CareNoteIcons.ArrowUp
    VitalLevel.Low -> CareNoteIcons.ArrowDown
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

private const val TRANSCRIPT_MAX_LINES = 3
private const val TILES_PER_ROW = 2
