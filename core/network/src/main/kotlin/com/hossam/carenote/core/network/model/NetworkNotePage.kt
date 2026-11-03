package com.hossam.carenote.core.network.model

import com.hossam.carenote.core.network.NotePage
import kotlinx.serialization.Serializable

/**
 * One page of `GET notes`. [cursor] marks where this page ends, in the order the server received the notes,
 * not by their createdAt: a note written offline yesterday and received today still comes after today's.
 */
@Serializable
data class NetworkNotePage(val notes: List<NetworkCareNote>, val cursor: String, val hasMore: Boolean)

/** Throws if a note's id or time can't be read, like the note mapper it uses. */
internal fun NetworkNotePage.asExternalModel() =
    NotePage(notes = notes.map { it.asExternalModel() }, cursor = cursor, hasMore = hasMore)
