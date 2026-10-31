package com.hossam.carenote.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Sizes of elements, as opposed to the [Spacing] between them. */
@Immutable
data class Dimens(
    /** Smallest size for anything tappable; staff often wear gloves. */
    val touchTarget: Dp = 48.dp,
    val recordButton: Dp = 96.dp,
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 24.dp,
    val iconLg: Dp = 48.dp,
    val borderThin: Dp = 1.dp,
    val elevationLow: Dp = 1.dp,
    val elevationMid: Dp = 3.dp,
)

internal val LocalDimens = staticCompositionLocalOf { Dimens() }
