package com.hossam.carenote.core.ui.mvi

/** Everything a screen shows, as one immutable value. */
interface UiState

/** Something the user did on a screen. */
interface UiIntent

/** Something that must happen once, such as navigating or showing a snackbar; not part of [UiState]. */
interface UiEffect
