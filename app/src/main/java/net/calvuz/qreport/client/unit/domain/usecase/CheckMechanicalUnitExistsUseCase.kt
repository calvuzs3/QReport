package net.calvuz.qreport.client.unit.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Checks that a mechanical unit exists and is active.
 *
 * Returns [QrResult.Success(MechanicalUnit)] if found and active.
 * Returns [QrError.UnitError.NotFound] if not found.
 * Returns [QrError.UnitError.AlreadyDeleted] if inactive.
 */
class CheckMechanicalUnitExistsUseCase @Inject constructor(
    private val repository: MechanicalUnitRepository
) {
    suspend operator fun invoke(
        unitId: String
    ): QrResult<MechanicalUnit, QrError.UnitError> {

        Timber.d("Check mechanical unit exists")

        // Check input
        if (unitId.isBlank()) {
            Timber.d("Unit id is blank")
            return QrResult.Error(QrError.UnitError.NotFound())
        }

        // Check
        return repository.getMechanicalUnitById(unitId).fold(
            onSuccess = { unit ->
                when {
                    unit == null -> {
                        Timber.d("Mechanical unit not found: $unitId")
                        QrResult.Error(QrError.UnitError.NotFound())
                    }
                    !unit.isActive -> {
                        Timber.d("Mechanical unit already is not active")
                        QrResult.Error(QrError.UnitError.AlreadyDeleted())
                    }
                    else -> {
                        Timber.d("Successfully retrived mechanical unit: $unit")
                        QrResult.Success(unit)
                    }
                }
            },
            onFailure = {
                Timber.d("Error in retrieving mechanical unit: ${it.message}")
                QrResult.Error(QrError.UnitError.LoadError(it.message)) }
        )
    }
}