package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.NoteAlreadyRevisedException
import com.hossam.carenote.core.domain.repository.StorageException
import com.hossam.carenote.core.domain.repository.SyncScheduler
import com.hossam.carenote.core.domain.vitals.VitalsProblem
import com.hossam.carenote.core.domain.vitals.VitalsValidator
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Vitals
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

sealed interface SaveResult {
    data class Saved(val noteId: UUID) : SaveResult

    /** Nothing was saved; the caller shows each problem next to its field. */
    data class Invalid(val problems: Set<VitalsProblem>) : SaveResult

    /** Neither words nor vitals: there's no note to save. */
    data object Empty : SaveResult

    /** The note being edited is gone, belongs to another resident, or was already edited; nothing was saved. */
    data object CannotRevise : SaveResult

    /** The phone's storage failed; nothing was saved, and trying again may work. */
    data object Failed : SaveResult
}

/**
 * Saves a new note locally as PENDING, queued for sync in the same transaction.
 * Validates again instead of trusting the screen, so the rule holds for every caller.
 *
 * Editing works the same way: with [revisionOf] set, the edit is saved as a new note pointing at the
 * one it replaces, which stays unchanged. Sync stays insert-only, and the record keeps every version.
 * Only the current version can be edited, so the history never splits in two. That's checked first for a
 * clear answer, and storage enforces it too, for two edits saved at the same moment.
 */
class SaveCareNoteUseCase @Inject constructor(
    private val careNoteRepository: CareNoteRepository,
    private val vitalsValidator: VitalsValidator,
    private val clock: Clock,
    private val syncScheduler: SyncScheduler,
) {
    suspend operator fun invoke(
        residentId: String,
        transcript: String,
        vitals: Vitals,
        revisionOf: UUID? = null,
    ): SaveResult = try {
        save(residentId, transcript, vitals, revisionOf)
    } catch (error: NoteAlreadyRevisedException) {
        SaveResult.CannotRevise
    } catch (error: StorageException) {
        SaveResult.Failed
    }

    private suspend fun save(residentId: String, transcript: String, vitals: Vitals, revisionOf: UUID?): SaveResult {
        val text = transcript.trim()
        if (text.isEmpty() && vitals.isEmpty) return SaveResult.Empty
        if (revisionOf != null && !canRevise(revisionOf, residentId)) return SaveResult.CannotRevise

        val problems = vitalsValidator.validate(vitals)
        if (problems.isNotEmpty()) return SaveResult.Invalid(problems)

        // The id is made here, on the device, so every sync retry sends the same one.
        val note = CareNote(
            id = UUID.randomUUID(),
            residentId = residentId,
            transcript = text,
            vitals = vitals,
            createdAt = clock.instant(),
            revisionOf = revisionOf,
        )
        careNoteRepository.save(note)
        requestSync()
        return SaveResult.Saved(note.id)
    }

    /**
     * Sent as soon as the device is online; saving itself never waits for the network. The note is already
     * stored, so a scheduling error must not report the save as failed, which would invite a duplicate:
     * the next app start or network change asks for a sync again and sends it then.
     */
    private fun requestSync() {
        try {
            syncScheduler.requestSync()
        } catch (error: Exception) {
            // Picked up by the next sync request; see the KDoc.
        }
    }

    private suspend fun canRevise(noteId: UUID, residentId: String): Boolean {
        val original = careNoteRepository.getNote(noteId) ?: return false
        return original.residentId == residentId && !careNoteRepository.hasRevision(noteId)
    }
}
