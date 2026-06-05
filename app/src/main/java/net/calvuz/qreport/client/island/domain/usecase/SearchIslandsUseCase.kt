package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Searches and filters robotic islands.
 *
 * The main [invoke] operator return [QrResult].
 * Utility methods (analytics, pattern search) keep [Result] since they
 * are not directly consumed by ViewModels.
 */
class SearchIslandsUseCase @Inject constructor(
    private val islandRepository: IslandRepository
) {
    /**
     * Text search across serial number, custom name and location.
     * Minimum query length: 2 characters.
     */
    suspend operator fun invoke(query: String): QrResult<List<Island>, QrError> {

        Timber.d("Search island")

        val trimmed = query.trim()
        if (trimmed.length < 2) {
            return QrResult.Error(QrError.IslandError.InvalidQueryLength())
        }

        return islandRepository.searchIslands(trimmed).fold(
            onSuccess = { islands -> QrResult.Success(islands.sortedByRelevance(trimmed)) },
            onFailure = { QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }

    private fun List<Island>.sortedByRelevance(query: String): List<Island> =
        sortedWith(
            compareBy<Island> { !it.serialNumber.equals(query, ignoreCase = true) }
                .thenBy { !it.serialNumber.startsWith(query, ignoreCase = true) && it.customName?.startsWith(query, ignoreCase = true) != true }
                .thenBy { it.serialNumber.lowercase() }
        )
}