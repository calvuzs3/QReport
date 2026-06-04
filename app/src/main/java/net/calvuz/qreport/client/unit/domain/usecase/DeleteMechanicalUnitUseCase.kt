package net.calvuz.qreport.client.unit.domain.usecase

import jakarta.inject.Inject
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import timber.log.Timber

/**
 * Two-stage soft-delete for a MechanicalUnit.
 * No children to cascade — only the unit row is affected.
 *
 * Stage 1 — DEACTIVATE: isActive=true  → isActive=false
 * Stage 2 — MARK DELETED: isActive=false, isDeleted=false → isDeleted=true
 */
class DeleteMechanicalUnitUseCase @Inject constructor(
    private val unitRepository: MechanicalUnitRepository,
    private val checkUnitExists: CheckMechanicalUnitExistsUseCase
) {
    suspend operator fun invoke(unitId: String): QrResult<String, QrError.UnitError> {

        Timber.Forest.d("Deleting (deactivating) unit $unitId")

        if (unitId.isBlank())
            return QrResult.Error(QrError.UnitError.NotFound())

        val unit = when (val r = checkUnitExists(unitId)) {
            is QrResult.Error -> {
                Timber.Forest.d("Error in deleting unit: ${r.error}")
                return QrResult.Error(r.error)
            }
            is QrResult.Success -> {
                Timber.Forest.d("Unit found: ${r.data}")
                r.data
            }
        }

        return when {
            unit.isActive -> unitRepository.deactivateUnit(unitId).fold(
                onSuccess = { QrResult.Success(unitId) },
                onFailure = { QrResult.Error(QrError.UnitError.DeleteError(it.message)) }
            )
            else -> QrResult.Error(QrError.UnitError.AlreadyDeleted())
        }
    }
}