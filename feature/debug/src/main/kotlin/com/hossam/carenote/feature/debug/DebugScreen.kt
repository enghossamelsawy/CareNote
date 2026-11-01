package com.hossam.carenote.feature.debug

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.ui.mvi.ObserveEffects

@Composable
fun DebugRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: DebugViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            DebugEffect.NavigateBack -> onBack()
        }
    }
    DebugScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** Mock flavor only: makes the fake server offline, slow or failing, to show sync and retry live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(state: DebugState, onIntent: (DebugIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(DebugIntent.BackClicked) }) {
                        Icon(
                            imageVector = CareNoteIcons.ArrowBack,
                            contentDescription = stringResource(R.string.debug_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
            SectionTitle(R.string.debug_section_server)
            ListItem(
                headlineContent = { Text(stringResource(R.string.debug_server_offline)) },
                supportingContent = { Text(stringResource(R.string.debug_server_offline_hint)) },
                trailingContent = {
                    Switch(
                        checked = state.serverOffline,
                        onCheckedChange = { onIntent(DebugIntent.ServerOfflineChanged(it)) },
                    )
                },
            )
            ChoiceRow(
                label = R.string.debug_failure_rate,
                options = FailureRate.entries,
                selected = state.failureRate,
                optionLabel = { stringResource(R.string.debug_percent, (it.share * PERCENT).toInt()) },
                onSelect = { onIntent(DebugIntent.FailureRateSelected(it)) },
            )
            ChoiceRow(
                label = R.string.debug_latency,
                options = Latency.entries,
                selected = state.latency,
                optionLabel = { stringResource(R.string.debug_latency_range, it.millis.first, it.millis.last) },
                onSelect = { onIntent(DebugIntent.LatencySelected(it)) },
            )

            HorizontalDivider()
            SectionTitle(R.string.debug_section_speech)
            ListItem(
                headlineContent = { Text(stringResource(R.string.debug_speech_engine)) },
                supportingContent = {
                    Text(state.speechEngine ?: stringResource(R.string.debug_speech_engine_none))
                },
            )

            HorizontalDivider()
            SectionTitle(R.string.debug_section_received)
            ListItem(
                headlineContent = { Text(stringResource(R.string.debug_received_count, state.receivedNoteCount)) },
            )
            state.recentNotes.forEach { note ->
                ListItem(
                    headlineContent = { Text(note.residentId) },
                    supportingContent = { Text(note.createdAt) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(title),
        modifier = modifier.padding(
            start = CareNoteTheme.spacing.md,
            top = CareNoteTheme.spacing.md,
            end = CareNoteTheme.spacing.md,
        ),
        style = CareNoteTheme.typography.titleSmall,
        color = CareNoteTheme.colors.primary,
    )
}

/** One setting with a few fixed choices, shown as chips so the current one is visible at a glance. */
@Composable
private fun <T> ChoiceRow(
    @StringRes label: Int,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = CareNoteTheme.spacing.md, vertical = CareNoteTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.xs),
    ) {
        Text(text = stringResource(label), style = CareNoteTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(optionLabel(option)) },
                )
            }
        }
    }
}

private const val PERCENT = 100

@PreviewLightDark
@Composable
private fun DebugScreenPreview() {
    CareNoteTheme {
        DebugScreen(
            state = DebugState(
                failureRate = FailureRate.Quarter,
                speechEngine = "ML Kit",
                receivedNoteCount = 2,
                recentNotes = listOf(
                    ReceivedNote(residentId = "res-101", createdAt = "2026-11-01T09:30:00Z"),
                    ReceivedNote(residentId = "res-104", createdAt = "2026-11-01T08:10:00Z"),
                ),
            ),
            onIntent = {},
        )
    }
}
