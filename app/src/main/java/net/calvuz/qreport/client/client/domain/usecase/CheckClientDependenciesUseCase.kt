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
 * Returns [QrError.ClientError.CannotDeleteHasDependencies] with dependency
 * counts if any active dependencies exist — counts are resolved to localised
 * strings in the UI layer via [QrErrorExt].
 */
class CheckClientDependenciesUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val facilityRepository: FacilityRepository,
    private val getContactsCount: GetContactsCountByClientUseCase,
    private val getContractsCount: GetContractsCountByClientUseCase
) {
    suspend operator fun invoke(clientId: String): QrResult<Unit, QrError.ClientError> {

        var facilitiesCount = 0
        var contactsCount = 0
        var contractsCount = 0
        var islandsCount = 0

        facilityRepository.getFacilitiesCountByClient(clientId)
            .onSuccess { count -> facilitiesCount = count }
            .onFailure { return QrResult.Error(QrError.ClientError.LoadError(it.message)) }

        when (val result = getContactsCount(clientId)) {
            is QrResult.Success -> contactsCount = result.data
            is QrResult.Error -> return QrResult.Error(QrError.ClientError.LoadError(result.error.toString()))
        }

        when (val result = getContractsCount(clientId)) {
            is QrResult.Success -> contractsCount = result.data
            is QrResult.Error -> return QrResult.Error(QrError.ClientError.LoadError(result.error.toString()))
        }

        clientRepository.getIslandsCount(clientId)
            .onSuccess { count -> islandsCount = count }
            .onFailure { return QrResult.Error(QrError.ClientError.LoadError(it.message)) }

        val hasDependencies = facilitiesCount > 0 || contactsCount > 0 ||
                contractsCount > 0 || islandsCount > 0

        return if (hasDependencies) {
            QrResult.Error(
                QrError.ClientError.CannotDeleteHasDependencies(
                    facilitiesCount = facilitiesCount,
                    contactsCount = contactsCount,
                    contractsCount = contractsCount,
                    islandsCount = islandsCount
                )
            )
        } else {
            QrResult.Success(Unit)
        }
    }
}