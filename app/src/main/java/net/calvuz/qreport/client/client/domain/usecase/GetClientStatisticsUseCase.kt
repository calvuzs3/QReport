package net.calvuz.qreport.client.client.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.client.presentation.model.ClientStatistics
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns statistics for a single client (used in list cards and detail screen).
 *
 * CheckUp statistics are optional: if [checkUpRepository] is null or the call
 * fails, zeroed placeholders are used and the use case still succeeds.
 */
class GetClientStatisticsUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val checkClientExists: CheckClientExistsUseCase,
    private val checkUpRepository: CheckUpRepository? = null
) {
    suspend operator fun invoke(clientId: String): QrResult<ClientStatistics, QrError.ClientError> {

        Timber.d("Getting statistics for client $clientId")

        if (clientId.isBlank()) {
            Timber.d("Client ID is blank")
            return QrResult.Error(QrError.ClientError.NotFound())
        }

        // Check client exists
        when (val clientCheck = checkClientExists(clientId)) {
            is QrResult.Success -> Unit
            is QrResult.Error -> return QrResult.Error(clientCheck.error)
        }

        val facilitiesCount = clientRepository.getFacilitiesCount(clientId)
            .getOrElse { return QrResult.Error(QrError.ClientError.LoadError(it.message)) }

        val contactsCount = clientRepository.getContactsCount(clientId)
            .getOrElse { return QrResult.Error(QrError.ClientError.LoadError(it.message)) }

        val contractsCount = clientRepository.getContractsCount(clientId)
            .getOrElse { return QrResult.Error(QrError.ClientError.LoadError(it.message)) }

        val islandsCount = clientRepository.getIslandsCount(clientId)
            .getOrElse { return QrResult.Error(QrError.ClientError.LoadError(it.message)) }

        // CheckUp stats are optional — failures produce zeroed placeholders
        val (totalCheckUps, completedCheckUps, lastCheckUpDate) = try {
            // TODO: uncomment when CheckUpRepository.getCheckUpsByClient() is available
            // val checkUps = checkUpRepository?.getCheckUpsByClient(clientId)?.getOrElse { emptyList() } ?: emptyList()
            // Triple(checkUps.size, checkUps.count { it.status.isCompleted() }, checkUps.maxByOrNull { it.updatedAt }?.updatedAt)
            Triple(0, 0, null)
        } catch (_: Exception) {
            Triple(0, 0, null)
        }

        return QrResult.Success(
            ClientStatistics(
                facilitiesCount = facilitiesCount,
                islandsCount = islandsCount,
                contactsCount = contactsCount,
                contractsCount = contractsCount,
                totalCheckUps = totalCheckUps,
                completedCheckUps = completedCheckUps,
                lastCheckUpDate = lastCheckUpDate
            )
        )
    }
}