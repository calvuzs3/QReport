package net.calvuz.qreport.client.unit.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Restores a facility and — if its parent client is inactive — restores the
 * client as well. Both operations run inside a single repository transaction.
 *
 * No cascade to child islands or units: only the facility itself (and its
 * parent when needed) is reactivated.
 */
@Suppress("HardcodedStringLiteral")
class RestoreUnitUseCase @Inject constructor(
    private val unitRepository: MechanicalUnitRepository,
    private val getUnitByIdUseCase: GetUnitByIdUseCase
) {
    suspend operator fun invoke(islandId: String): QrResult<Unit, QrError.UnitError> {

        if (islandId.isBlank()) {
            Timber.d("Island ID is blank")
            return QrResult.Error(QrError.UnitError.NotFound())
        }

        // Load facility
        when (val r = getUnitByIdUseCase(islandId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // Restore facility — and parent if inactive
        return unitRepository.restoreUnit(
            id = islandId,
        ).fold(
            onSuccess = {
                Timber.d("Successfully restored island $islandId")
                QrResult.Success(Unit)
            },
            onFailure = {
                Timber.e(it, "Failed to restore island $islandId")
                QrResult.Error(QrError.UnitError.DeleteError(it.message))
            }
        )
    }
}