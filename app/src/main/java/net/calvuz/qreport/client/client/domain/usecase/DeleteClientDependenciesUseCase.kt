package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.repository.ContactRepository
import net.calvuz.qreport.client.contact.domain.usecase.GetContactsByClientUseCase
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Soft-deletes all dependencies of a client (facilities, contacts).
 *
 * Called by [DeleteClientUseCase] when cascade = true.
 * Uses repositories directly to bypass dependency-check logic in use cases.
 */
class DeleteClientDependenciesUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val contactRepository: ContactRepository,
    private val getContactsByClient: GetContactsByClientUseCase
) {
    suspend operator fun invoke(clientId: String): QrResult<Unit, QrError.ClientError> {

        // Soft-delete facilities (cascade deletes islands at DB level)
        facilityRepository.getFacilitiesByClient(clientId)
            .onSuccess { facilities ->
                facilities.forEach { facility ->
                    facilityRepository.softDeleteFacility(facility.id)
                        .onFailure {
                            return QrResult.Error(QrError.ClientError.DeleteError(it.message))
                        }
                }
            }
            .onFailure {
                return QrResult.Error(QrError.ClientError.DeleteError(it.message))
            }

        // Soft-delete contacts
        when (val contacts = getContactsByClient(clientId)) {
            is QrResult.Error ->
                return QrResult.Error(QrError.ClientError.DeleteError(contacts.error.toString()))
            is QrResult.Success -> contacts.data.forEach { contact ->
                when (val result = contactRepository.deleteContact(contact.id)) {
                    is QrResult.Error ->
                        return QrResult.Error(QrError.ClientError.DeleteError(result.error.toString()))
                    is QrResult.Success -> Unit
                }
            }
        }

        return QrResult.Success(Unit)
    }
}