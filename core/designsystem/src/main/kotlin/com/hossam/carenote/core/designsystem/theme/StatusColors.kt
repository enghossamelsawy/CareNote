package com.hossam.carenote.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors for states Material 3 has no role for, such as "waiting to sync" or "saved".
 * Each one is a container color with a matching text color, so status is never shown by color alone.
 */
@Immutable
data class StatusColors(
    val neutral: Color,
    val onNeutral: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    /** A small mark meaning "all fine", such as a dot or a check, drawn straight on a surface. */
    val successIndicator: Color,
)

internal val LightStatusColors = StatusColors(
    neutral = Color(0xFFDAE5E4),
    onNeutral = Color(0xFF3F4948),
    warning = Color(0xFFFFDDB3),
    onWarning = Color(0xFF2A1800),
    error = Color(0xFFFFDAD6),
    onError = Color(0xFF410002),
    success = Color(0xFFB7F1B9),
    onSuccess = Color(0xFF002107),
    successIndicator = Color(0xFF006E1C),
)

internal val DarkStatusColors = StatusColors(
    neutral = Color(0xFF3F4948),
    onNeutral = Color(0xFFDAE5E4),
    warning = Color(0xFF653E00),
    onWarning = Color(0xFFFFDDB3),
    error = Color(0xFF93000A),
    onError = Color(0xFFFFDAD6),
    success = Color(0xFF005316),
    onSuccess = Color(0xFFB7F1B9),
    successIndicator = Color(0xFF7BDA82),
)

internal val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
