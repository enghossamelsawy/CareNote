package com.hossam.carenote.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme

/**
 * The big round button that starts and stops dictation. Large enough to hit with gloves on;
 * the icon and the colour both change, so the state never relies on colour alone.
 * [contentDescription] should say what tapping does now, e.g. "Stop recording".
 */
@Composable
fun RecordButton(isRecording: Boolean, onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(CareNoteTheme.dimens.recordButton),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isRecording) colors.error else colors.primary,
            contentColor = if (isRecording) colors.onError else colors.onPrimary,
        ),
    ) {
        Icon(
            imageVector = if (isRecording) CareNoteIcons.Stop else CareNoteIcons.Mic,
            contentDescription = contentDescription,
            modifier = Modifier.size(CareNoteTheme.dimens.iconLg),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecordButtonPreview() {
    CareNoteTheme {
        Surface {
            Row(
                modifier = Modifier.padding(CareNoteTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.lg),
            ) {
                RecordButton(isRecording = false, onClick = {}, contentDescription = "Start recording")
                RecordButton(isRecording = true, onClick = {}, contentDescription = "Stop recording")
            }
        }
    }
}
