package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

class DeleteClientDependenciesUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val facilityRepository: FacilityRepository,
    private val contactRepository: ContactRepository,
){

    /**
     * Elimina tutte le dipendenze del cliente
     */
    suspend operator fun invoke(clientId: String): Result<Unit> {
        return try {
            // Elimina facilities (che a cascata eliminerà le isole)
            facilityRepository.getFacilitiesByClient(clientId)
                .onSuccess { facilities ->
                    facilities.forEach { facility ->
                        facilityRepository.deleteFacility(facility.id)
                            .onFailure { return Result.failure(it) }
                    }
                }
                .onFailure { return Result.failure(it) }

            // Elimina contatti
            contactRepository.getContactsByClient(clientId)
                .onSuccess { contacts ->
                    contacts.forEach { contact ->
                        contactRepository.deleteContact(contact.id)
                            .onFailure { return Result.failure(it) }
                    }
                }
                .onFailure { return Result.failure(it) }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}