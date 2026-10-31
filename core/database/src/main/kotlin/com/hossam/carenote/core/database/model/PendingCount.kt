package com.hossam.carenote.core.database.model

/** How many of a resident's notes are not on the server yet. A query result, not a table. */
data class PendingCount(val residentId: String, val count: Int)
