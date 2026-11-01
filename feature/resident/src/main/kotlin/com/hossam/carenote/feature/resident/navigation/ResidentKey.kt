package com.hossam.carenote.feature.resident.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** One resident's detail screen. Serializable, so the back stack survives process death. */
@Serializable
data class ResidentKey(val residentId: String) : NavKey
