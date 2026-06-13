package net.calvuz.qreport.client.unit.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns a single [MechanicalUnit] by ID.
 */
class GetUnitByIdUseCase @Inject constructor(
    private val unitRepository: MechanicalUnitRepository,
) {
    suspend operator fun invoke(unitId: String): QrResult<MechanicalUnit, QrError.UnitError> {

        Timber.v("Get unit by id")
        
        if (unitId.isBlank()) {
            Timber.d("UnitId is blank")
            return QrResult.Error(QrError.UnitError.NotFound())
        }

        return unitRepository.getUnitById(unitId).fold(
            onSuccess = { unit ->
                if (unit != null) {
                    Timber.d("Successfully retrieved unit $unit")
                    QrResult.Success(unit)
                }
                else {
                    Timber.d("Error retrieving unit $unitId")
                    QrResult.Error(QrError.UnitError.NotFound())
                }
            },
            onFailure = {
                Timber.d("Error in deleting unit ${it.message}")
                QrResult.Error(QrError.UnitError.LoadError(it.message)) }
        )
    }
}