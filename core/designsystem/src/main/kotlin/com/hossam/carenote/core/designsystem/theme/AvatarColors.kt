package com.hossam.carenote.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** One avatar's background with a readable text color on it. */
@Immutable
data class AvatarColor(val container: Color, val content: Color)

/**
 * Background colors for initials avatars. Only cool hues, so an avatar is never mistaken for a
 * green, orange or red status next to it.
 */
@Immutable
data class AvatarColors(val palette: List<AvatarColor>) {
    /** The same [key], such as a resident id, always gets the same color, also after a restart. */
    fun forKey(key: String): AvatarColor = palette[Math.floorMod(key.hashCode(), palette.size)]
}

internal val LightAvatarColors = AvatarColors(
    palette = listOf(
        AvatarColor(container = Color(0xFFB2EBF2), content = Color(0xFF00363D)),
        AvatarColor(container = Color(0xFFC2E8FF), content = Color(0xFF001E2C)),
        AvatarColor(container = Color(0xFFD6E3FF), content = Color(0xFF001B3E)),
        AvatarColor(container = Color(0xFFDFE0FF), content = Color(0xFF000F5C)),
        AvatarColor(container = Color(0xFFF0DBFF), content = Color(0xFF2C0051)),
        AvatarColor(container = Color(0xFFFFD7F0), content = Color(0xFF3A0030)),
    ),
)

internal val DarkAvatarColors = AvatarColors(
    palette = listOf(
        AvatarColor(container = Color(0xFF004F57), content = Color(0xFFB2EBF2)),
        AvatarColor(container = Color(0xFF004C69), content = Color(0xFFC2E8FF)),
        AvatarColor(container = Color(0xFF1B4689), content = Color(0xFFD6E3FF)),
        AvatarColor(container = Color(0xFF2F3E92), content = Color(0xFFDFE0FF)),
        AvatarColor(container = Color(0xFF5B2C82), content = Color(0xFFF0DBFF)),
        AvatarColor(container = Color(0xFF7A1F65), content = Color(0xFFFFD7F0)),
    ),
)

internal val LocalAvatarColors = staticCompositionLocalOf { LightAvatarColors }
