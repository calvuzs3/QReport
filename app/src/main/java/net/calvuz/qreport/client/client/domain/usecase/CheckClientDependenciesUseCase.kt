package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.contact.domain.usecase.GetContactsCountByClientUseCase
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

class CheckClientDependenciesUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val facilityRepository: FacilityRepository,
    private val contacts: GetContactsCountByClientUseCase
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
                        dependencies.add("$count facilities")
                    }
                }
                .onFailure { return Result.failure(it) }

            // Controllo contatti
            when (val count = contacts(clientId)) {
                is QrResult.Success -> {
                    if (count.data > 0) {
                        dependencies.add("$count contacts")
                    }
                }
                is QrResult.Error -> {
                    return Result.failure(Throwable(count.error.toString()))
                }
            }
//                .onSuccess { count ->
//                    if (count > 0) {
//                        dependencies.add("$count contatto/i")
//                    }
//                }
//                .onFailure { return Result.failure(it) }

            // Controllo isole (tramite client)
            clientRepository.getIslandsCount(clientId)
                .onSuccess { count ->
                    if (count > 0) {
                        dependencies.add("$count islands")
                    }
                }
                .onFailure { return Result.failure(it) }

            if (dependencies.isNotEmpty()) {
                val dependencyText = dependencies.joinToString(", ")
                throw IllegalStateException(
                    "Cannot delete client: there are $dependencyText. " +
                            "use force=true to delete dependencies"
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}