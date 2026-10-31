package com.hossam.carenote.core.model

/** Where a note is on its way to the server. Every note starts as [PENDING]. */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
}
