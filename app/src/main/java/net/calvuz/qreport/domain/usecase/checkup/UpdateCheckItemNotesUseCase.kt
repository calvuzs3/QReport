package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.repository.CheckItemRepository
import javax.inject.Inject

/**
 * Use Case per aggiornare le note di un check item
 *
 * Gestisce:
 * - Aggiornamento note con validazione
 * - Persistenza nel database
 * - Gestione errori
 */
class UpdateCheckItemNotesUseCase @Inject constructor(
    private val checkItemRepository: CheckItemRepository
) {
    suspend operator fun invoke(
        itemId: String,
        notes: String
    ): Result<Unit> {
        return try {
            // Trim delle note per rimuovere spazi vuoti
            val cleanedNotes = notes.trim()

            // Aggiorna le note del check item
            checkItemRepository.updateCheckItemNotes(itemId, cleanedNotes)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}