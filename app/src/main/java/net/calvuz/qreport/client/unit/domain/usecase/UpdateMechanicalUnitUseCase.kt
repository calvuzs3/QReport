package net.calvuz.qreport.client.unit.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Updates an existing [MechanicalUnit], refreshing its [MechanicalUnit.updatedAt] timestamp.
 *
 * Verifies the unit exists before updating.
 * The island ID must not change — it is immutable after creation.
 */
class UpdateMechanicalUnitUseCase @Inject constructor(
    private val repository: MechanicalUnitRepository,
    private val checkUnitExists: CheckMechanicalUnitExistsUseCase
) {
    suspend operator fun invoke(unit: MechanicalUnit): QrResult<Unit, QrError.UnitError> {

        Timber.d("Update mechanical unit")

        if (unit.name.isBlank()) {
            Timber.d("Mechanical unit name is blank")
            return QrResult.Error(QrError.UnitError.MissingName())
        }

        // Verify mechanical unit exists
        val original = when (val check = checkUnitExists(unit.id)) {
            is QrResult.Error -> {
                Timber.d("Mechanical unit exists failed: ${check.error}")
                return QrResult.Error(check.error)
            }
            is QrResult.Success -> check.data
        }

        // Island must not change
        if (unit.islandId != original.islandId) {
            Timber.d("Tryinmg to change island paternity: ${unit.islandId}-${original.islandId}")
            return QrResult.Error(QrError.UnitError.InvalidField())
        }

        val updated = unit.copy(updatedAt = Clock.System.now())
        return repository.update(updated).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.UnitError.UpdateError(it.message)) }
        )
    }
}