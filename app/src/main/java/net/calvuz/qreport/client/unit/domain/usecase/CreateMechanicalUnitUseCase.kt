package net.calvuz.qreport.client.unit.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.usecase.CheckIslandExistsUseCase
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Creates a new [MechanicalUnit] after verifying the parent island exists.
 */
class CreateMechanicalUnitUseCase @Inject constructor(
    private val repository: MechanicalUnitRepository,
    private val checkIslandExists: CheckIslandExistsUseCase
) {
    suspend operator fun invoke(unit: MechanicalUnit): QrResult<Unit, QrError.UnitError> {

        Timber.d("Create mechanical unit")

        // Check input
        if (unit.islandId.isBlank()) {
            Timber.d("Island id is blank")
            return QrResult.Error(QrError.UnitError.IslandNotFound())
        }
        if (unit.name.isBlank()) {
            Timber.d("Mechanical unit name is blank")
            return QrResult.Error(QrError.UnitError.MissingName())
        }

        // Verify parent island exists
        when (checkIslandExists(unit.islandId)) {
            is QrResult.Error -> {
                Timber.d("Island not found: ${unit.islandId}")
                return QrResult.Error(QrError.UnitError.IslandNotFound())
            }
            is QrResult.Success -> Unit
        }

        // Create
        return repository.create(unit).fold(
            onSuccess = {
                Timber.d("Mechanical unit successfully created: ${Unit}")
                QrResult.Success(Unit) },
            onFailure = {
                Timber.d("Error in creating mechanical unit; ${it.message}")
                QrResult.Error(QrError.UnitError.CreateError(it.message)) }
        )
    }
}