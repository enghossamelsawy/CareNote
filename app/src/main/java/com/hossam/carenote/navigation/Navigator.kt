package com.hossam.carenote.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Changes the back stack only on behalf of the screen on top. A quick second tap arrives after the
 * first one already opened the next screen, so it no longer comes from the top and is dropped.
 */
internal class Navigator(private val backStack: MutableList<NavKey>) {
    /** Opens [to] while [from] is still the top screen. */
    fun navigate(from: NavKey, to: NavKey) {
        if (backStack.lastOrNull() == from) backStack.add(to)
    }

    /** Closes the top screen, but never the first one, so a repeated back press can't empty the stack. */
    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
