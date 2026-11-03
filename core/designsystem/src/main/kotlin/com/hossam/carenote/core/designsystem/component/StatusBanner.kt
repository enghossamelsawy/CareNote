package com.hossam.carenote.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme

/**
 * A full-width strip under the top bar for a state that affects the whole screen, such as being offline.
 * Screen readers announce it when it appears. [actionLabel] adds a button at the end, for a state the user
 * can do something about.
 */
@Composable
fun StatusBanner(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val (container, content) = tone.colors()
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = container,
        contentColor = content,
    ) {
        // The button brings its own padding, so the end gap is smaller with one.
        Row(
            modifier = Modifier.padding(
                start = CareNoteTheme.spacing.md,
                end = if (actionLabel != null) CareNoteTheme.spacing.xs else CareNoteTheme.spacing.md,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f).padding(vertical = CareNoteTheme.spacing.sm),
                style = CareNoteTheme.typography.bodyMedium,
            )
            if (actionLabel != null) {
                TextButton(onClick = onAction, colors = ButtonDefaults.textButtonColors(contentColor = content)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StatusBannerPreview() {
    CareNoteTheme {
        StatusBanner(text = "Offline. New notes sync when you're back online.", tone = StatusTone.Warning)
    }
}

@PreviewLightDark
@Composable
private fun StatusBannerWithActionPreview() {
    CareNoteTheme {
        StatusBanner(
            text = "Not updated since 7:15 AM. Colleagues' notes may be missing.",
            tone = StatusTone.Warning,
            actionLabel = "Update now",
        )
    }
}
