package com.hossam.carenote.core.model

/** A resident as shown in the residents list, with the number of their notes not yet on the server. */
data class ResidentSummary(val resident: Resident, val pendingCount: Int)
