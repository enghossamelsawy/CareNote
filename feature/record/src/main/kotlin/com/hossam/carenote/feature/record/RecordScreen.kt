package com.hossam.carenote.feature.record

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hossam.carenote.core.designsystem.component.RecordButton
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.ui.mvi.ObserveEffects

/**
 * Runs the Android side of recording for [RecordViewModel]: the permission dialog, the app settings and haptics.
 * Every decision stays in the ViewModel.
 */
@Composable
fun RecordRoute(
    residentId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = hiltViewModel<RecordViewModel, RecordViewModel.Factory>(
        creationCallback = { factory -> factory.create(residentId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val haptics = LocalHapticFeedback.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val canAskAgain = activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) ?: false
        viewModel.onIntent(RecordIntent.PermissionResult(granted = granted, canAskAgain = canAskAgain))
    }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            RecordEffect.RequestMicPermission -> {
                val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.onIntent(RecordIntent.PermissionResult(granted = true, canAskAgain = true))
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            RecordEffect.OpenAppSettings -> {
                val appDetails = Uri.fromParts("package", context.packageName, null)
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appDetails))
            }

            is RecordEffect.Haptic -> haptics.performHapticFeedback(
                when (effect.kind) {
                    HapticKind.Start -> HapticFeedbackType.ToggleOn
                    HapticKind.Stop -> HapticFeedbackType.ToggleOff
                },
            )

            RecordEffect.NavigateBack -> onBack()
        }
    }

    // Never keep the microphone open in the background: it's private, and Android cuts it off anyway.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onIntent(RecordIntent.StopClicked)
    }

    RecordScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** Dictating a note. Stateless: everything comes in through [state], everything goes out through [onIntent]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(state: RecordState, onIntent: (RecordIntent) -> Unit, modifier: Modifier = Modifier) {
    val isRecording = state.phase == RecordPhase.Recording
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.residentName.isBlank()) {
                            stringResource(R.string.record_title_fallback)
                        } else {
                            stringResource(R.string.record_title, state.residentName)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(RecordIntent.BackClicked) }) {
                        Icon(
                            imageVector = CareNoteIcons.ArrowBack,
                            contentDescription = stringResource(R.string.record_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(CareNoteTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.md),
        ) {
            Transcript(state = state, modifier = Modifier.weight(1f).fillMaxWidth())

            PermissionCard(permission = state.permission, onIntent = onIntent)

            state.error?.let { error ->
                Text(
                    text = stringResource(error.message()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            RecordButton(
                isRecording = isRecording,
                onClick = { onIntent(if (isRecording) RecordIntent.StopClicked else RecordIntent.RecordClicked) },
                contentDescription = stringResource(if (isRecording) R.string.record_stop else R.string.record_start),
            )
        }
    }
}

@Composable
private fun Transcript(state: RecordState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.verticalScroll(rememberScrollState())) {
        if (state.transcript.isBlank()) {
            Text(
                text = stringResource(
                    if (state.phase == RecordPhase.Recording) R.string.record_listening else R.string.record_hint,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(text = state.transcript, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** Shown only after a refusal: explains why, and offers the one action that can still help. */
@Composable
private fun PermissionCard(permission: MicPermission, onIntent: (RecordIntent) -> Unit, modifier: Modifier = Modifier) {
    val (text, action, intent) = when (permission) {
        MicPermission.Denied -> Triple(
            R.string.record_permission_rationale,
            R.string.record_permission_allow,
            RecordIntent.RecordClicked,
        )

        MicPermission.PermanentlyDenied -> Triple(
            R.string.record_permission_settings_text,
            R.string.record_permission_open_settings,
            RecordIntent.OpenSettingsClicked,
        )

        MicPermission.Unknown, MicPermission.Granted -> return
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(CareNoteTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
        ) {
            Text(text = stringResource(text), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onIntent(intent) }) {
                Text(stringResource(action))
            }
        }
    }
}

private fun RecordError.message(): Int = when (this) {
    RecordError.NoSpeech -> R.string.record_error_no_speech
    RecordError.Busy -> R.string.record_error_busy
    RecordError.Unavailable -> R.string.record_error_unavailable
    RecordError.Unknown -> R.string.record_error_unknown
}

@PreviewLightDark
@Composable
private fun RecordScreenIdlePreview() {
    CareNoteTheme {
        RecordScreen(state = RecordState(residentName = "Anna Becker"), onIntent = {})
    }
}

@PreviewLightDark
@Composable
private fun RecordScreenRecordingPreview() {
    CareNoteTheme {
        RecordScreen(
            state = RecordState(
                residentName = "Anna Becker",
                phase = RecordPhase.Recording,
                transcript = "Blood pressure 120 over 80, pulse",
                permission = MicPermission.Granted,
            ),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun RecordScreenDonePreview() {
    CareNoteTheme {
        RecordScreen(
            state = RecordState(
                residentName = "Anna Becker",
                phase = RecordPhase.Done,
                transcript = "Blood pressure 120 over 80, pulse 72, temperature 36.8",
                permission = MicPermission.Granted,
            ),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun RecordScreenPermissionDeniedPreview() {
    CareNoteTheme {
        RecordScreen(
            state = RecordState(residentName = "Anna Becker", permission = MicPermission.Denied),
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun RecordScreenErrorPreview() {
    CareNoteTheme {
        RecordScreen(
            state = RecordState(
                residentName = "Anna Becker",
                error = RecordError.NoSpeech,
                permission = MicPermission.Granted,
            ),
            onIntent = {},
        )
    }
}
