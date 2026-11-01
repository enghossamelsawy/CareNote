package com.hossam.carenote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme

/**
 * One measured value, such as "Pulse 72 bpm". A normal value stays calm: a neutral tile with a small
 * green dot. Only a value that [needsAttention] gets color, with its [level] ("High") written out,
 * so red is the one thing that stands out on a screen of notes.
 * Screen readers read the tile as one item and always hear the level, also when it isn't shown.
 */
@Composable
fun VitalTile(
    label: String,
    value: String,
    unit: String,
    level: String,
    needsAttention: Boolean,
    modifier: Modifier = Modifier,
    levelIcon: ImageVector? = null,
) {
    val status = CareNoteTheme.statusColors
    val colors = CareNoteTheme.colors
    val container = if (needsAttention) status.error else colors.surfaceContainerHighest
    val content = if (needsAttention) status.onError else colors.onSurface
    val secondary = if (needsAttention) status.onError else colors.onSurfaceVariant
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            if (!needsAttention) stateDescription = level
        },
        shape = CareNoteTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = CareNoteTheme.spacing.md, vertical = CareNoteTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.xxs),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = CareNoteTheme.typography.labelMedium,
                    color = secondary,
                )
                if (!needsAttention) {
                    Box(
                        modifier = Modifier
                            .size(CareNoteTheme.dimens.indicatorDot)
                            .background(CareNoteTheme.statusColors.successIndicator, CircleShape),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.xs)) {
                Text(
                    text = value,
                    modifier = Modifier.alignByBaseline(),
                    style = CareNoteTheme.typography.titleMedium,
                )
                Text(
                    text = unit,
                    modifier = Modifier.alignByBaseline(),
                    style = CareNoteTheme.typography.labelMedium,
                    color = secondary,
                )
            }
            if (needsAttention) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (levelIcon != null) {
                        Icon(
                            imageVector = levelIcon,
                            contentDescription = null,
                            modifier = Modifier.size(CareNoteTheme.dimens.iconSm),
                        )
                    }
                    Text(text = level, style = CareNoteTheme.typography.labelMedium)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun VitalTilePreview() {
    CareNoteTheme {
        Surface {
            Row(
                modifier = Modifier.padding(CareNoteTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
            ) {
                VitalTile(
                    label = "Temperature",
                    value = "38.4",
                    unit = "°C",
                    level = "High",
                    needsAttention = true,
                    modifier = Modifier.weight(1f),
                    levelIcon = CareNoteIcons.ArrowUp,
                )
                VitalTile(
                    label = "Pulse",
                    value = "72",
                    unit = "bpm",
                    level = "Normal",
                    needsAttention = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
