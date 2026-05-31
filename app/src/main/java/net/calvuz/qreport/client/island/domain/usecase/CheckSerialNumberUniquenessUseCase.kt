package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import javax.inject.Inject

/**
 * Checks that a serial number is not already taken globally.
 *
 * Returns [QrResult.Success(Unit)] if available.
 * Returns [QrError.IslandError.DuplicateSerialNumber] if taken.
 */
class CheckSerialNumberUniquenessUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {
    suspend operator fun invoke(serialNumber: String): QrResult<Unit, QrError.IslandError> {
        if (serialNumber.isBlank()) {
            return QrResult.Error(QrError.IslandError.MissingSerialNumber())
        }

        return islandRepository.isSerialNumberTaken(serialNumber).fold(
            onSuccess = { isTaken ->
                if (isTaken) QrResult.Error(QrError.IslandError.DuplicateSerialNumber())
                else QrResult.Success(Unit)
            },
            onFailure = { QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }
}