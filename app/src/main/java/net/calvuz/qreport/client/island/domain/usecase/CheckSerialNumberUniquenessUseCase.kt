package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Checks that a serial number is not already taken globally.
 *
 * Returns [QrResult.Success(Unit)] if available.
 * Returns [QrError.IslandError.ValidationError.DuplicateSerialNumber] if taken.
 */
class CheckSerialNumberUniquenessUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {
    suspend operator fun invoke(serialNumber: String): QrResult<Unit, QrError.IslandError> {

        Timber.d("Check serial number uniqueness")

        if (serialNumber.isBlank()) {
            Timber.d("Serial number is blank")
            return QrResult.Error(QrError.IslandError.MissingSerialNumber())
        }

        return islandRepository.isSerialNumberTaken(serialNumber).fold(
            onSuccess = { isTaken ->
                if (isTaken) {
                    Timber.d("Serial number is already taken")
                    QrResult.Error(QrError.IslandError.ValidationError.DuplicateSerialNumber())
                }
                else {
                    Timber.d("Successfully proved serial number uniqueness")
                    QrResult.Success(Unit)
                }
            },
            onFailure = {
                Timber.d("Error in serial number uniqueness: ${it.message}")
                QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }
}