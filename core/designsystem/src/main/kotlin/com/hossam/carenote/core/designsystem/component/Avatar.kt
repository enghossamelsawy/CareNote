package com.hossam.carenote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme

enum class AvatarSize { Small, Large }

/**
 * A circle with the person's initials, colored by [colorKey] so the same person always looks the
 * same. Needs no image or network, so it works offline. Screen readers skip it: the name it stands
 * for is always shown next to it.
 */
@Composable
fun Avatar(name: String, colorKey: String, modifier: Modifier = Modifier, size: AvatarSize = AvatarSize.Small) {
    val color = CareNoteTheme.avatarColors.forKey(colorKey)
    Box(
        modifier = modifier
            .size(
                when (size) {
                    AvatarSize.Small -> CareNoteTheme.dimens.avatarSm
                    AvatarSize.Large -> CareNoteTheme.dimens.avatarLg
                },
            )
            .clip(CircleShape)
            .background(color.container)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            style = when (size) {
                AvatarSize.Small -> CareNoteTheme.typography.titleMedium
                AvatarSize.Large -> CareNoteTheme.typography.headlineSmall
            },
            color = color.content,
            maxLines = 1,
        )
    }
}

/** "Anna Maria Becker" gives "AB": the first letter of the first and last word. */
internal fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val letters = when (words.size) {
        0 -> emptyList()
        1 -> listOf(words.first())
        else -> listOf(words.first(), words.last())
    }
    // By code point, so a letter outside the basic plane isn't cut in half.
    return letters.joinToString(separator = "") { it.substring(0, it.offsetByCodePoints(0, 1)) }.uppercase()
}

@PreviewLightDark
@Composable
private fun AvatarPreview() {
    CareNoteTheme {
        Surface {
            Row(
                modifier = Modifier.padding(CareNoteTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(CareNoteTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(name = "Anna Becker", colorKey = "1")
                Avatar(name = "Karl Fischer", colorKey = "2")
                Avatar(name = "Greta Hoffmann", colorKey = "3")
                Avatar(name = "Anna Becker", colorKey = "1", size = AvatarSize.Large)
            }
        }
    }
}
