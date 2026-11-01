package com.hossam.carenote.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme

/**
 * A full-width strip under the top bar for a state that affects the whole screen, such as being offline.
 * Screen readers announce it when it appears.
 */
@Composable
fun StatusBanner(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val (container, content) = tone.colors()
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = container,
        contentColor = content,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = CareNoteTheme.spacing.md, vertical = CareNoteTheme.spacing.sm),
            style = CareNoteTheme.typography.bodyMedium,
        )
    }
}

@PreviewLightDark
@Composable
private fun StatusBannerPreview() {
    CareNoteTheme {
        StatusBanner(text = "Offline. New notes sync when you're back online.", tone = StatusTone.Warning)
    }
}
