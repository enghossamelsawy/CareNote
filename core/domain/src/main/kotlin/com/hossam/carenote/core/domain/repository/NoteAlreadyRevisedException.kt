package com.hossam.carenote.core.domain.repository

import java.util.UUID

/**
 * An edit of [noteId] was refused because another edit of it was saved first. Only the latest version can
 * be edited, and storage enforces that too, so two edits saved at the same moment can't split the history.
 */
class NoteAlreadyRevisedException(val noteId: UUID) : Exception("Note $noteId already has an edit")
