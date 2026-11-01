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
 * green dot. A value that [needsAttention] keeps the same size and layout, with a soft red tint, the
 * value in red and its [level] ("High") written where the dot would be, so every tile lines up.
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
    val container = if (needsAttention) status.errorSubtle else colors.surfaceContainerHighest
    val valueColor = if (needsAttention) status.errorIndicator else colors.onSurface
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            if (!needsAttention) stateDescription = level
        },
        shape = CareNoteTheme.shapes.medium,
        color = container,
        contentColor = colors.onSurface,
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
                    color = colors.onSurfaceVariant,
                )
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
                                tint = status.errorIndicator,
                            )
                        }
                        Text(text = level, style = CareNoteTheme.typography.labelMedium, color = status.errorIndicator)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(CareNoteTheme.dimens.indicatorDot)
                            .background(status.successIndicator, CircleShape),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.xs)) {
                Text(
                    text = value,
                    modifier = Modifier.alignByBaseline(),
                    style = CareNoteTheme.typography.titleMedium,
                    color = valueColor,
                )
                Text(
                    text = unit,
                    modifier = Modifier.alignByBaseline(),
                    style = CareNoteTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
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
