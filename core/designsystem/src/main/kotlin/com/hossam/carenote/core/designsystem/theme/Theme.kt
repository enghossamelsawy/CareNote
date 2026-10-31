package com.hossam.carenote.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The app's theme: Material 3 colors, type and shapes, plus the CareNote tokens in [CareNoteTheme].
 * Dynamic color is off on purpose: the wallpaper must not change what "pending" or "failed" looks
 * like, so the colors are the same on every device.
 */
@Composable
fun CareNoteTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalDimens provides Dimens(),
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = CareNoteTypography,
            shapes = CareNoteShapes,
            content = content,
        )
    }
}

/** The tokens Material 3 doesn't have. Colors, type and shapes come from [MaterialTheme]. */
object CareNoteTheme {
    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val dimens: Dimens
        @Composable
        @ReadOnlyComposable
        get() = LocalDimens.current

    val statusColors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}
