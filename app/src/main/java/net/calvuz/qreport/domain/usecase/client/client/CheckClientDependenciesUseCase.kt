package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.repository.ClientRepository
import net.calvuz.qreport.domain.repository.ContactRepository
import net.calvuz.qreport.domain.repository.FacilityRepository
import javax.inject.Inject

class CheckClientDependenciesUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val facilityRepository: FacilityRepository,
    private val contactRepository: ContactRepository
){

    /**
     * Controlla se esistono dipendenze che impediscono l'eliminazione
     */
    suspend operator fun invoke(clientId: String): Result<Unit> {
        return try {
            val dependencies = mutableListOf<String>()

            // Controllo facilities
            facilityRepository.getFacilitiesCountByClient(clientId)
                .onSuccess { count ->
                    if (count > 0) {
                        dependencies.add("$count stabilimento/i")
                    }
                }
                .onFailure { return Result.failure(it) }

            // Controllo contatti
            contactRepository.getContactsCountByClient(clientId)
                .onSuccess { count ->
                    if (count > 0) {
                        dependencies.add("$count contatto/i")
                    }
                }
                .onFailure { return Result.failure(it) }

            // Controllo isole (tramite client)
            clientRepository.getIslandsCount(clientId)
                .onSuccess { count ->
                    if (count > 0) {
                        dependencies.add("$count isola/e robotizzata/e")
                    }
                }
                .onFailure { return Result.failure(it) }

            if (dependencies.isNotEmpty()) {
                val dependencyText = dependencies.joinToString(", ")
                throw IllegalStateException(
                    "Impossibile eliminare cliente: sono presenti $dependencyText. " +
                            "Utilizzare force=true per eliminare anche le dipendenze."
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}