package com.hossam.carenote.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme

/** What a [StatusBadge] says about its subject; features map their own states onto these. */
enum class StatusTone { Neutral, Warning, Error, Success }

/**
 * A small pill with a short label such as "3 pending" or "Synced". The text always carries the
 * meaning; the color only adds to it, so the badge also works for people who can't tell colors apart.
 */
@Composable
fun StatusBadge(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val colors = CareNoteTheme.statusColors
    val (container, content) = when (tone) {
        StatusTone.Neutral -> colors.neutral to colors.onNeutral
        StatusTone.Warning -> colors.warning to colors.onWarning
        StatusTone.Error -> colors.error to colors.onError
        StatusTone.Success -> colors.success to colors.onSuccess
    }
    Surface(modifier = modifier, shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = CareNoteTheme.spacing.sm, vertical = CareNoteTheme.spacing.xxs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@PreviewLightDark
@Composable
private fun StatusBadgePreview() {
    CareNoteTheme {
        Surface {
            Column(
                modifier = Modifier.padding(CareNoteTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
            ) {
                StatusBadge(text = "Draft", tone = StatusTone.Neutral)
                StatusBadge(text = "3 pending", tone = StatusTone.Warning)
                StatusBadge(text = "Sync failed", tone = StatusTone.Error)
                StatusBadge(text = "Synced", tone = StatusTone.Success)
            }
        }
    }
}
