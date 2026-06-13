package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Restores a facility and — if its parent client is inactive — restores the
 * client as well. Both operations run inside a single repository transaction.
 *
 * No cascade to child islands or units: only the facility itself (and its
 * parent when needed) is reactivated.
 */
@Suppress("HardcodedStringLiteral")
class RestoreIslandUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val getIslandByIdUseCase: GetIslandByIdUseCase
) {
    suspend operator fun invoke(islandId: String): QrResult<Unit, QrError.IslandError> {

        if (islandId.isBlank()) {
            Timber.d("Island ID is blank")
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        // Load facility
        when (val r = getIslandByIdUseCase(islandId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // Restore facility — and parent if inactive
        return islandRepository.restoreIsland(
            id = islandId,
        ).fold(
            onSuccess = {
                Timber.d("Successfully restored island $islandId")
                QrResult.Success(Unit)
            },
            onFailure = {
                Timber.e(it, "Failed to restore island $islandId")
                QrResult.Error(QrError.IslandError.DeleteError(it.message))
            }
        )
    }
}