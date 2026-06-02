package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
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

    suspend operator fun invoke(
        islandId: String
    ): QrResult<Island, QrError.IslandError> {

        Timber.d("Check island exists")

        if (islandId.isBlank()) {
            Timber.d("Island id is blank")
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        return islandRepository.getIslandById(islandId).fold(
            onSuccess = { island ->
                when {
                    island == null -> {
                        Timber.d("Island not found: $islandId")
                        QrResult.Error(QrError.IslandError.NotFound())
                    }
                    !island.isActive -> {
                        Timber.d("Island is not active: $islandId")
                        QrResult.Error(QrError.IslandError.AlreadyDeleted())
                    }
                    else -> {
                        Timber.d("Island successfully retrieved: $island")
                        QrResult.Success(island)
                    }
                }
            },
            onFailure = {
                Timber.d("Error in retriving island: $islandId")
                QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }
}