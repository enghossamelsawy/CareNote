package com.hossam.carenote.feature.residents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hossam.carenote.core.designsystem.component.Avatar
import com.hossam.carenote.core.designsystem.component.EmptyState
import com.hossam.carenote.core.designsystem.component.EmptyStateIcon
import com.hossam.carenote.core.designsystem.component.StatusBadge
import com.hossam.carenote.core.designsystem.component.StatusBanner
import com.hossam.carenote.core.designsystem.component.StatusTone
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.domain.sync.Freshness
import com.hossam.carenote.core.domain.vitals.VitalLevel
import com.hossam.carenote.core.domain.vitals.VitalsAlert
import com.hossam.carenote.core.domain.vitals.VitalsAssessment
import com.hossam.carenote.core.ui.mvi.ObserveEffects
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
            // With no residents, the empty state already says what is missing.
            if (state.residents.isNotEmpty()) {
                state.freshness?.let { FreshnessLine(it, onUpdateNow = { onIntent(ResidentsIntent.UpdateNowClicked) }) }
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

        // While downloading there's no button, so a second download can't be started; a problem says why.
        state.residents.isEmpty() -> EmptyState(
            title = stringResource(R.string.residents_empty_title),
            message = stringResource(
                when {
                    state.isRefreshing -> R.string.residents_downloading
                    state.refreshProblem == RefreshProblem.Unavailable -> R.string.residents_unreachable
                    state.refreshProblem == RefreshProblem.Failed -> R.string.residents_download_failed
                    else -> R.string.residents_empty_message
                },
            ),
            illustration = { EmptyStateIcon(CareNoteIcons.Group) },
            modifier = modifier,
            actionLabel = if (state.isRefreshing) null else stringResource(R.string.residents_empty_action),
            onAction = { onIntent(ResidentsIntent.RetryClicked) },
        )

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
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.xs)) {
                Text(stringResource(R.string.residents_room, resident.room))
                resident.alert?.let { VitalsAlertLine(it) }
                resident.overdueSince?.let { OverdueLine(it) }
            }
        },
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

/** Which vitals are out of range, in red, and when they were measured, so an old reading reads as old. */
@Composable
private fun VitalsAlertLine(alert: VitalsAlert, modifier: Modifier = Modifier) {
    val formatter = rememberTimeFormatter()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.xxs)) {
        StatusBadge(text = alertText(alert.assessment), tone = StatusTone.Error)
        Text(
            text = stringResource(R.string.residents_alert_taken, formatter.format(alert.takenAt)),
            style = CareNoteTheme.typography.labelMedium,
            color = CareNoteTheme.colors.onSurfaceVariant,
        )
    }
}

/**
 * "No note since 6:10 AM", muted instead of red: a reminder that a check is due, not a sign that something is
 * wrong, so it doesn't compete with a vitals alert.
 */
@Composable
private fun OverdueLine(since: Instant, modifier: Modifier = Modifier) {
    val formatter = rememberTimeFormatter()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = CareNoteIcons.Schedule,
            contentDescription = null,
            modifier = Modifier.size(CareNoteTheme.dimens.iconSm),
            tint = CareNoteTheme.colors.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.residents_overdue, formatter.format(since)),
            style = CareNoteTheme.typography.labelMedium,
            color = CareNoteTheme.colors.onSurfaceVariant,
        )
    }
}

/**
 * Whether colleagues' notes are here. "Updated 10:42 AM" stays quiet, since that is the normal case; a warning
 * with "Update now" when they haven't arrived yet or are out of date, because the overdue and attention flags
 * may then be wrong.
 */
@Composable
private fun FreshnessLine(freshness: Freshness, onUpdateNow: () -> Unit, modifier: Modifier = Modifier) {
    when (freshness) {
        is Freshness.Updated -> {
            val formatter = remember {
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
            }
            Text(
                text = stringResource(R.string.residents_updated, formatter.format(freshness.at)),
                modifier = modifier.padding(horizontal = CareNoteTheme.spacing.md, vertical = CareNoteTheme.spacing.xs),
                style = CareNoteTheme.typography.labelMedium,
                color = CareNoteTheme.colors.onSurfaceVariant,
            )
        }

        Freshness.NeverUpdated -> StatusBanner(
            text = stringResource(R.string.residents_never_updated),
            tone = StatusTone.Warning,
            modifier = modifier,
            actionLabel = stringResource(R.string.residents_update_now),
            onAction = onUpdateNow,
        )

        is Freshness.OutOfDate -> StatusBanner(
            text = stringResource(R.string.residents_out_of_date, rememberTimeFormatter().format(freshness.since)),
            tone = StatusTone.Warning,
            modifier = modifier,
            actionLabel = stringResource(R.string.residents_update_now),
            onAction = onUpdateNow,
        )
    }
}

/** Date and time in the phone's zone and format, such as "Nov 1, 2026, 6:10 AM". */
@Composable
private fun rememberTimeFormatter(): DateTimeFormatter = remember {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault())
}

/** "Temperature high, Pulse high": every vital out of range, in the order the note screen shows them. */
@Composable
private fun alertText(assessment: VitalsAssessment): String = listOfNotNull(
    assessment.bloodPressure.flag(high = R.string.residents_alert_bp_high, low = R.string.residents_alert_bp_low),
    assessment.pulse.flag(high = R.string.residents_alert_pulse_high, low = R.string.residents_alert_pulse_low),
    assessment.temperature.flag(
        high = R.string.residents_alert_temperature_high,
        low = R.string.residents_alert_temperature_low,
    ),
    R.string.residents_alert_spo2_low.takeIf { assessment.oxygenSaturation == VitalLevel.Low },
).map { stringResource(it) }.joinToString(stringResource(R.string.residents_alert_separator))

private fun VitalLevel?.flag(high: Int, low: Int): Int? = when (this) {
    VitalLevel.High -> high
    VitalLevel.Low -> low
    VitalLevel.Normal, null -> null
}

@PreviewLightDark
@Composable
private fun ResidentsScreenPreview() {
    CareNoteTheme {
        ResidentsScreen(
            state = ResidentsState(
                isLoading = false,
                residents = listOf(
                    ResidentItem(
                        id = "1",
                        name = "Anna Becker",
                        room = "101",
                        pendingCount = 2,
                        alert = VitalsAlert(
                            assessment = VitalsAssessment(temperature = VitalLevel.High),
                            takenAt = Instant.parse("2026-11-01T05:29:00Z"),
                        ),
                    ),
                    ResidentItem(
                        id = "2",
                        name = "Karl Fischer",
                        room = "102",
                        pendingCount = 0,
                        overdueSince = Instant.parse("2026-10-31T20:40:00Z"),
                    ),
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
private fun ResidentsScreenOutOfDatePreview() {
    CareNoteTheme {
        ResidentsScreen(
            state = ResidentsState(
                isLoading = false,
                residents = listOf(ResidentItem(id = "1", name = "Anna Becker", room = "101", pendingCount = 0)),
                freshness = Freshness.OutOfDate(since = Instant.parse("2026-11-01T07:15:00Z")),
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
