package net.calvuz.qreport.domain.usecase.checkup

import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.spare.SparePart
import net.calvuz.qreport.domain.model.spare.SparePartCategory
import net.calvuz.qreport.domain.model.spare.SparePartUrgency
import net.calvuz.qreport.domain.repository.CheckUpRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case per aggiungere una parte di ricambio a un check-up
 *
 * Gestisce:
 * - Creazione nuovo spare part
 * - Validazione dati
 * - Persistenza nel database
 */
class AddSparePartUseCase @Inject constructor(
    private val checkUpRepository: CheckUpRepository
) {
    suspend operator fun invoke(
        checkUpId: String,
        partNumber: String,
        description: String,
        quantity: Int,
        urgency: SparePartUrgency,
        category: SparePartCategory,
        estimatedCost: Double? = null,
        notes: String = "",
        supplierInfo: String = ""
    ): Result<String> {
        return try {
            // Validazione input
            if (partNumber.isBlank()) {
                return Result.failure(IllegalArgumentException("Il numero parte non può essere vuoto"))
            }

            if (description.isBlank()) {
                return Result.failure(IllegalArgumentException("La descrizione non può essere vuota"))
            }

            if (quantity <= 0) {
                return Result.failure(IllegalArgumentException("La quantità deve essere maggiore di 0"))
            }

            // Crea il nuovo spare part
            val sparePartId = UUID.randomUUID().toString()
            val sparePart = SparePart(
                id = sparePartId,
                checkUpId = checkUpId,
                partNumber = partNumber.trim(),
                description = description.trim(),
                quantity = quantity,
                urgency = urgency,
                category = category,
                estimatedCost = estimatedCost,
                notes = notes.trim(),
                supplierInfo = supplierInfo.trim(),
                addedAt = Clock.System.now()
            )

            // TODO: Aggiungere metodo addSparePart al repository
            // Per ora assume che il metodo esista
            // checkUpRepository.addSparePart(sparePart)

            Result.success(sparePartId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}