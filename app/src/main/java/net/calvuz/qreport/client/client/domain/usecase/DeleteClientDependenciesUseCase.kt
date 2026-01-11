package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.contact.domain.usecase.DeleteContactUseCase
import net.calvuz.qreport.client.contact.domain.usecase.GetContactsByClientUseCase
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

class DeleteClientDependenciesUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val contactRepository: ContactRepository,
    private val getContactsByClientUseCase: GetContactsByClientUseCase
) {

    /**
     * Delete Client's dependencies
     *
     * we use contact repository in order to avoid
     * checks on delete by the use case
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
            when (val contacts = getContactsByClientUseCase(clientId)) {
                is QrResult.Error -> {
                    return Result.failure(Throwable(contacts.error.toString()))
                }

                is QrResult.Success -> {
                    contacts.data.forEach { contact ->
                        when (val result = contactRepository.deleteContact(contact.id)) {
                            is QrResult.Error -> {
                                return Result.failure(Throwable(result.error.toString()))
                            }

                            is QrResult.Success -> {}
                        }
                    }
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}