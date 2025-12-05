package net.calvuz.qreport.domain.usecase.client.facility

import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.repository.FacilityRepository
import javax.inject.Inject

class HandlePrimaryFacilityChangeUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
){

    /**
     * Gestisce il cambio di facility primaria prima della creazione
     */
    suspend operator fun invoke(clientId: String): Result<Unit> {
        return try {
            // Se esiste già una facility primaria, rimuovi il flag
            val existingPrimary = facilityRepository.getPrimaryFacility(clientId)
                .getOrElse { return Result.success(Unit) } // Non c'è facility primaria esistente

            if (existingPrimary != null) {
                // Aggiorna la facility esistente rimuovendo il flag primario
                val updatedPrimary = existingPrimary.copy(
                    isPrimary = false,
                    updatedAt = Clock.System.now()
                )
                facilityRepository.updateFacility(updatedPrimary)
                    .onFailure { return Result.failure(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}