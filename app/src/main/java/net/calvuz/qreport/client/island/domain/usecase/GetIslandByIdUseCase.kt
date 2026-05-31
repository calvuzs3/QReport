package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import javax.inject.Inject

/**
 * Returns a single [Island] by ID.
 */
class GetIslandByIdUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {
    suspend operator fun invoke(islandId: String): QrResult<Island, QrError.IslandError> {
        if (islandId.isBlank()) {
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        return islandRepository.getIslandById(islandId).fold(
            onSuccess = { island ->
                if (island != null) QrResult.Success(island)
                else QrResult.Error(QrError.IslandError.NotFound())
            },
            onFailure = { QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }
}