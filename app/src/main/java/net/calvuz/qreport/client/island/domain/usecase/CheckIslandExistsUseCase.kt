package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import javax.inject.Inject

/**
 * Checks that an island exists and is active.
 *
 * Returns [QrResult.Success(Island)] if found and active.
 * Returns [QrError.IslandError.NotFound] if not found.
 * Returns [QrError.IslandError.AlreadyDeleted] if inactive.
 */
class CheckIslandExistsUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {
    suspend operator fun invoke(islandId: String): QrResult<Island, QrError.IslandError> {
        if (islandId.isBlank()) {
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        return islandRepository.getIslandById(islandId).fold(
            onSuccess = { island ->
                when {
                    island == null -> QrResult.Error(QrError.IslandError.NotFound())
                    !island.isActive -> QrResult.Error(QrError.IslandError.AlreadyDeleted())
                    else -> QrResult.Success(island)
                }
            },
            onFailure = { QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }
}