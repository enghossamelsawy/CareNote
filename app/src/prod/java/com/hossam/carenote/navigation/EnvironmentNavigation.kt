package com.hossam.carenote.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/** Prod flavor: no debug menu, so no button for it and no screen behind it. */
internal val debugMenuKey: NavKey? = null

@Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
internal fun EntryProviderScope<NavKey>.environmentEntries(onBack: () -> Unit) = Unit
