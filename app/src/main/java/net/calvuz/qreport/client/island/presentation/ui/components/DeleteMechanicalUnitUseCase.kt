package net.calvuz.qreport.client.island.presentation.ui.components

import jakarta.inject.Inject
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import net.calvuz.qreport.client.unit.domain.usecase.CheckMechanicalUnitExistsUseCase
import timber.log.Timber

/**
 * Two-stage soft-delete for a MechanicalUnit.
 * No children to cascade — only the unit row is affected.
 *
 * Stage 1 — DEACTIVATE: isActive=true  → isActive=false
 * Stage 2 — MARK DELETED: isActive=false, isDeleted=false → isDeleted=true
 */
enum class DeleteUnitResult { DEACTIVATED, MARKED_DELETED }

class DeleteMechanicalUnitUseCase @Inject constructor(
    private val unitRepository: MechanicalUnitRepository,
    private val checkUnitExists: CheckMechanicalUnitExistsUseCase
) {
    suspend operator fun invoke(unitId: String): QrResult<DeleteUnitResult, QrError.UnitError> {

        Timber.d("Deleting (deactivating) unit $unitId")

        if (unitId.isBlank())
            return QrResult.Error(QrError.UnitError.NotFound())

        val unit = when (val r = checkUnitExists(unitId)) {
            is QrResult.Error -> {
                Timber.d("Error in deleting unit: ${r.error}")
                return QrResult.Error(r.error)
            }
            is QrResult.Success -> {
                Timber.d("Unit found: ${r.data}")
                r.data
            }
        }

        return when {
            unit.isActive -> unitRepository.deactivateUnit(unitId).fold(
                onSuccess = { QrResult.Success(DeleteUnitResult.DEACTIVATED) },
                onFailure = { QrResult.Error(QrError.UnitError.DeleteError(it.message)) }
            )
//            !unit.isActive && !unit.isDeleted -> unitRepository.markUnitDeleted(unitId).fold(
//                onSuccess = { QrResult.Success(DeleteUnitResult.MARKED_DELETED) },
//                onFailure = { QrResult.Error(QrError.UnitError.DeleteError(it.message)) }
//            )
            else -> QrResult.Error(QrError.UnitError.AlreadyDeleted())
        }
    }
}