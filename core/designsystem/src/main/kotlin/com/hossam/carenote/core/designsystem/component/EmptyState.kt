package com.hossam.carenote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hossam.carenote.core.designsystem.icon.CareNoteIcons
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme

/**
 * What a screen shows when it has nothing to list: an illustration, a [title] saying what's missing,
 * a [message] saying what to do about it, and an optional action button. Scrolls, so large text is
 * never cut off.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    illustration: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CareNoteTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        illustration()
        Text(
            text = title,
            modifier = Modifier.padding(top = CareNoteTheme.spacing.sm),
            style = CareNoteTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = CareNoteTheme.typography.bodyMedium,
            color = CareNoteTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            OutlinedButton(onClick = onAction, modifier = Modifier.padding(top = CareNoteTheme.spacing.sm)) {
                Text(actionLabel)
            }
        }
    }
}

/** The usual [EmptyState] illustration: an icon in a soft circle. */
@Composable
fun EmptyStateIcon(imageVector: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(CareNoteTheme.dimens.emptyStateIllustration)
            .background(CareNoteTheme.colors.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(CareNoteTheme.dimens.iconLg),
            tint = CareNoteTheme.colors.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun EmptyStatePreview() {
    CareNoteTheme {
        Surface {
            EmptyState(
                title = "No residents yet",
                message = "The list downloads when the phone is online.",
                illustration = { EmptyStateIcon(CareNoteIcons.Group) },
                actionLabel = "Try again",
            )
        }
    }
}
