package com.hossam.carenote.feature.record

import androidx.lifecycle.SavedStateHandle

/**
 * The unfinished note, kept in the screen's saved state so it survives Android stopping the app in the
 * background. Only plain strings and string lists go in, so the system can store them without help.
 *
 * Saved state is held by the system outside the app's database, so a draft lives there only until the
 * note is saved or the screen is left.
 */
internal class RecordDraft(private val handle: SavedStateHandle) {
    /** Keeps what [state] holds, or forgets the draft when there is nothing worth keeping. */
    fun keep(state: RecordState) {
        when {
            // The draft from before Save stays: it's cleared once the note is stored, and the state
            // stays "saving" after that, so writing it again here would bring a saved note back.
            state.isSaving -> Unit
            state.phase == RecordPhase.Idle || state.cannotEdit != null -> clear()
            else -> {
                handle[TRANSCRIPT] = state.transcript
                handle.setForm(VITALS, state.vitals)
                handle[EDITED_TRANSCRIPT] = state.editedFrom?.transcript
                state.editedFrom?.let { handle.setForm(EDITED_VITALS, it.vitals) }
            }
        }
    }

    /**
     * The note as it was, back in review. A recording cut short comes back with the words heard so far;
     * the microphone is never restarted on its own.
     */
    fun restore(): RecordState? {
        val transcript = handle.get<String>(TRANSCRIPT) ?: return null
        val editedFrom = handle.get<String>(EDITED_TRANSCRIPT)?.let { editedTranscript ->
            EditedNote(transcript = editedTranscript, vitals = handle.getForm(EDITED_VITALS))
        }
        return RecordState(
            phase = RecordPhase.Done,
            transcript = transcript,
            vitals = handle.getForm(VITALS),
            editedFrom = editedFrom,
        )
    }

    fun clear() {
        ALL_KEYS.forEach { handle.remove<Any>(it) }
    }

    // Field names and their texts as two lists, so a value cleared by hand comes back cleared too.
    private fun SavedStateHandle.setForm(key: String, form: VitalsForm) {
        this["$key.fields"] = ArrayList(form.values.keys.map { it.name })
        this["$key.texts"] = ArrayList(form.values.values)
    }

    private fun SavedStateHandle.getForm(key: String): VitalsForm {
        val fields = get<List<String>>("$key.fields").orEmpty().map(VitalField::valueOf)
        val texts = get<List<String>>("$key.texts").orEmpty()
        return VitalsForm(values = fields.zip(texts).toMap())
    }

    private companion object {
        const val TRANSCRIPT = "draft.transcript"
        const val VITALS = "draft.vitals"
        const val EDITED_TRANSCRIPT = "draft.editedTranscript"
        const val EDITED_VITALS = "draft.editedVitals"
        val ALL_KEYS = listOf(TRANSCRIPT, EDITED_TRANSCRIPT) +
            listOf(VITALS, EDITED_VITALS).flatMap { listOf("$it.fields", "$it.texts") }
    }
}
