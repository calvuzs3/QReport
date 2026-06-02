package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns a single [Island] by ID.
 */
class GetIslandByIdUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {
    suspend operator fun invoke(islandId: String): QrResult<Island, QrError.IslandError> {

        Timber.d("Get island by id")
        if (islandId.isBlank()) {
            Timber.d("Island id is blank")
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        return islandRepository.getIslandById(islandId).fold(
            onSuccess = { island ->
                if (island != null) {
                    Timber.d("Successfully retrieved island: $island")
                    QrResult.Success(island)
                }
                else {
                    Timber.d("Error island not found: $islandId")
                    QrResult.Error(QrError.IslandError.NotFound())
                }
            },
            onFailure = {
                Timber.d("Error in deleting islans: ${it.message}")
                QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }
}