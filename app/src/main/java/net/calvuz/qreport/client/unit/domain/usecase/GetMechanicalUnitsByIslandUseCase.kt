package net.calvuz.qreport.client.unit.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.usecase.CheckIslandExistsUseCase
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns the list of active [MechanicalUnit]s belonging to a given island.
 *
 * Verifies that the parent island exists before querying units.
 * For reactive UI use [ObserveMechanicalUnitsUseCase] instead.
 */
class GetMechanicalUnitsByIslandUseCase @Inject constructor(
    private val repository: MechanicalUnitRepository,
    private val checkIslandExists: CheckIslandExistsUseCase
) {
    suspend operator fun invoke(
        islandId: String
    ): QrResult<List<MechanicalUnit>, QrError.UnitError> {

        Timber.d("Get mechanical unit by island id")

        // Check input
        if (islandId.isBlank()) {
            Timber.d("MecIsland id is blank")
            return QrResult.Error(QrError.UnitError.IslandNotFound())
        }

        // Check island exists
        when (checkIslandExists(islandId)) {
            is QrResult.Error -> {
                Timber.d("Island not found for id: $islandId")
                return QrResult.Error(QrError.UnitError.IslandNotFound())
            }
            is QrResult.Success -> Unit
        }

        // Get
        return repository.getUnitsByIsland(islandId).fold(
            onSuccess = {
                Timber.d("Successfully retrieved mechanical unit: $it")
                QrResult.Success(it) },
            onFailure = {
                Timber.d("Error in retrieving mechanical unit: ${it.message}")
                QrResult.Error(QrError.UnitError.LoadError(it.message)) }
        )
    }
}