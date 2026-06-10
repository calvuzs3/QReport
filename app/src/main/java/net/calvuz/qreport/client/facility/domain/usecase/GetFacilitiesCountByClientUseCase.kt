package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.usecase.CheckClientExistsUseCase
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
import javax.inject.Inject

class GetFacilitiesCountByClientUseCase @Inject constructor(
    private val repository: FacilityRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {

    suspend operator fun invoke(clientId: String): QrResult<Int, QrError> {

        Timber.v("Getting facilities count for client: $clientId")

        try {

            // Check input
            if (clientId.isBlank()) {
                Timber.w("ClientId is blank")
                return QrResult.Error(QrError.ContactsError.MissingClientId())
            }

            // Check client exists
            when (val clientCheck = checkClientExists(clientId)) {
                is QrResult.Success -> Unit
                is QrResult.Error -> return QrResult.Error(clientCheck.error)
            }

            return repository.getFacilitiesCountByClient(clientId).fold(onSuccess = { count ->
                Timber.d("Retrieved $count contacts for client: $clientId")
                QrResult.Success(count)
            }, onFailure = {
                Timber.d(it, "Repository error for clientId $clientId")
                QrResult.Error(QrError.FacilityError.LoadError(it.message))
            })
        } catch (e: Exception) {
            Timber.e(e, "Get facilities count error for clientId $clientId")
            return QrResult.Error(QrError.SystemError.UnknownError())
        }
    }
}
