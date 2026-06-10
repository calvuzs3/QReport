package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Checks that a facility name is not already taken for the given client.
 *
 * Returns [QrResult.Success(Unit)] if the name is available.
 * Returns [QrError.FacilityError.CreateError] if already taken.
 */
class CheckFacilityNameUniquenessUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(
        clientId: String, name: String, excludeId: String = ""
    ): QrResult<Unit, QrError.FacilityError> {

        Timber.v("Checking facility name uniqueness: $name")

        if (name.isBlank()) {
            Timber.d("Facility name is blank")
            return QrResult.Error(QrError.FacilityError.MissingName())
        }

        return facilityRepository.isFacilityNameTakenForClient(clientId, name, excludeId)
            .fold(onSuccess = { isTaken ->
                Timber.d("Facility name ($name) is already taken")
                if (isTaken) QrResult.Error(QrError.FacilityError.CreateError())
                else QrResult.Success(Unit)
            }, onFailure = {
                Timber.e(it, "Failed to check facility name ($name) uniqueness")
                QrResult.Error(QrError.FacilityError.LoadError(it.message))
            })
    }
}