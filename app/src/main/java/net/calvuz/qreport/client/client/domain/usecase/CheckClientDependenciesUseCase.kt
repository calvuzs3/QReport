package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.contact.domain.usecase.GetContactsCountByClientUseCase
import net.calvuz.qreport.client.contract.domain.usecase.GetContractsCountByClientUseCase
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Checks whether a client has active dependencies that would block deletion.
 *
 * Returns [QrResult.Success(Unit)] if the client can be safely deleted.
 * Returns [QrError.ClientError.CannotDeleteHasActiveFacilities] if dependencies exist.
 */
class CheckClientDependenciesUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val facilityRepository: FacilityRepository,
    private val getContactsCount: GetContactsCountByClientUseCase,
    private val getContractsCount: GetContractsCountByClientUseCase
) {
    suspend operator fun invoke(clientId: String): QrResult<Unit, QrError.ClientError> {
        val dependencies = mutableListOf<String>()

        // Check facilities
        facilityRepository.getFacilitiesCountByClient(clientId)
            .onSuccess { count -> if (count > 0) dependencies.add("$count facilities") }
            .onFailure { return QrResult.Error(QrError.ClientError.LoadError(it.message)) }

        // Check contacts
        when (val result = getContactsCount(clientId)) {
            is QrResult.Success -> if (result.data > 0) dependencies.add("${result.data} contacts")
            is QrResult.Error -> return QrResult.Error(QrError.ClientError.LoadError(result.error.toString()))
        }

        // Check contracts
        when (val result = getContractsCount(clientId)) {
            is QrResult.Success -> if (result.data > 0) dependencies.add("${result.data} contracts")
            is QrResult.Error -> return QrResult.Error(QrError.ClientError.LoadError(result.error.toString()))
        }

        // Check islands
        clientRepository.getIslandsCount(clientId)
            .onSuccess { count -> if (count > 0) dependencies.add("$count islands") }
            .onFailure { return QrResult.Error(QrError.ClientError.LoadError(it.message)) }

        return if (dependencies.isNotEmpty()) {
            QrResult.Error(
                QrError.ClientError.CannotDeleteHasActiveFacilities(
                    "Cannot delete client: has ${dependencies.joinToString(", ")}"
                )
            )
        } else {
            QrResult.Success(Unit)
        }
    }
}