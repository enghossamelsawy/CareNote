package com.hossam.carenote.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The app's theme. Dynamic color is off on purpose: the wallpaper must not change what
 * "pending" or "failed" looks like, so the colors are the same on every device.
 */
@Composable
fun CareNoteTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = CareNoteTypography,
        content = content,
    )
}
