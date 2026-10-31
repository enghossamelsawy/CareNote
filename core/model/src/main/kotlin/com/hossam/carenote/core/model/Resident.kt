package com.hossam.carenote.core.model

/** A person living in the care home. Holds only what the app shows: no health records. */
data class Resident(
    /** Server id. */
    val id: String,
    val firstName: String,
    val lastName: String,
    val room: String,
)
