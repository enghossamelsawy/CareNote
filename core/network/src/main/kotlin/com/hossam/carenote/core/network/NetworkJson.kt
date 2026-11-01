package com.hossam.carenote.core.network

import kotlinx.serialization.json.Json

/**
 * The one JSON setup for talking to the server, shared with the mock server so both read and write the same.
 * Unknown keys are ignored, so a field the server adds later doesn't break older app versions.
 */
val careNoteJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
