package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Searches and filters robotic islands.
 *
 * The main [invoke] and [advancedSearch] operators return [QrResult].
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
    suspend operator fun invoke(query: String): QrResult<List<Island>, QrError.IslandError> {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            return QrResult.Error(QrError.IslandError.InvalidField("Search query must be at least 2 characters"))
        }

        return islandRepository.searchIslands(trimmed).fold(
            onSuccess = { islands -> QrResult.Success(islands.sortedByRelevance(trimmed)) },
            onFailure = { QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }

    suspend fun findBySerialNumber(serialNumber: String): QrResult<Island?, QrError.IslandError> {
        if (serialNumber.isBlank()) {
            return QrResult.Error(QrError.IslandError.MissingSerialNumber())
        }
        return islandRepository.getIslandBySerialNumber(serialNumber.trim()).fold(
            onSuccess = { QrResult.Success(it) },
            onFailure = { QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }

    suspend fun filterByType(islandType: IslandType): QrResult<List<Island>, QrError.IslandError> =
        islandRepository.getIslandsByType(islandType).fold(
            onSuccess = { QrResult.Success(it.sortedBy { island -> island.serialNumber.lowercase() }) },
            onFailure = { QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )

    suspend fun filterByClient(clientId: String): QrResult<List<Island>, QrError.IslandError> {
        if (clientId.isBlank()) {
            return QrResult.Error(QrError.IslandError.FacilityNotFound("Client ID is required"))
        }
        return islandRepository.getIslandsByClient(clientId).fold(
            onSuccess = { islands ->
                QrResult.Success(islands.sortedWith(
                    compareBy<Island> { it.islandType.name }.thenBy { it.serialNumber.lowercase() }
                ))
            },
            onFailure = { QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }

    suspend fun advancedSearch(criteria: IslandSearchCriteria): QrResult<List<Island>, QrError.IslandError> {
        val allIslands = islandRepository.getActiveIslands().fold(
            onSuccess = { it },
            onFailure = { return QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )

        val now = Clock.System.now()
        var filtered = allIslands

        criteria.textQuery?.takeIf { it.isNotBlank() }?.let { q ->
            filtered = filtered.filter {
                it.serialNumber.contains(q, ignoreCase = true) ||
                        it.customName?.contains(q, ignoreCase = true) == true ||
                        it.modelNumber?.contains(q, ignoreCase = true) == true || // modelNumber, not model
                        it.location?.contains(q, ignoreCase = true) == true
            }
        }
        criteria.islandType?.let { type -> filtered = filtered.filter { it.islandType == type } }
        criteria.facilityId?.takeIf { it.isNotBlank() }?.let { fid -> filtered = filtered.filter { it.facilityId == fid } }
        criteria.isActive?.let { active -> filtered = filtered.filter { it.isActive == active } }
        criteria.underWarranty?.let { uw ->
            filtered = filtered.filter { (it.warrantyExpiration?.let { exp -> exp > now } == true) == uw }
        }
        criteria.maintenanceOverdue?.let { od ->
            filtered = filtered.filter { it.needsMaintenance() == od }
        }
        criteria.operatingHoursMin?.let { min -> filtered = filtered.filter { it.operatingHours >= min } }
        criteria.operatingHoursMax?.let { max -> filtered = filtered.filter { it.operatingHours <= max } }
        criteria.cycleCountMin?.let { min -> filtered = filtered.filter { it.cycleCount >= min } }
        criteria.cycleCountMax?.let { max -> filtered = filtered.filter { it.cycleCount <= max } }

        return QrResult.Success(
            filtered.sortedWith(compareBy<Island> { it.islandType.name }.thenBy { it.serialNumber.lowercase() })
        )
    }

    // ── Utility methods (Result<T> kept — not consumed directly by ViewModels) ─

    suspend fun getIslandsRequiringAttention(currentTime: Instant? = null): Result<Map<String, List<Island>>> {
        val ts = currentTime ?: Clock.System.now()
        val maintenance = islandRepository.getIslandsRequiringMaintenance(ts).getOrElse { return Result.failure(it) }
        val warranty = islandRepository.getIslandsUnderWarranty(ts).getOrElse { return Result.failure(it) }
        val all = islandRepository.getActiveIslands().getOrElse { return Result.failure(it) }
        val thirtyDays = ts + 30.days
        return Result.success(mapOf(
            "maintenance_overdue" to maintenance,
            "warranty_expiring" to warranty.filter { it.warrantyExpiration?.let { exp -> exp <= thirtyDays } == true },
            "high_operating_hours" to all.filter { it.operatingHours > 8_000 },
            "high_cycle_count" to all.filter { it.cycleCount > 500_000L }
        ))
    }

    suspend fun searchBySerialPattern(pattern: String): Result<List<Island>> {
        if (pattern.isBlank()) return Result.failure(IllegalArgumentException("Pattern is required"))
        val regex = Regex(pattern.replace("*", ".*"), RegexOption.IGNORE_CASE)
        return islandRepository.getActiveIslands().map { islands ->
            islands.filter { regex.matches(it.serialNumber) }.sortedBy { it.serialNumber }
        }
    }

    // -------------------------------------------------------------------------

    private fun List<Island>.sortedByRelevance(query: String): List<Island> =
        sortedWith(
            compareBy<Island> { !it.serialNumber.equals(query, ignoreCase = true) }
                .thenBy { !it.serialNumber.startsWith(query, ignoreCase = true) && it.customName?.startsWith(query, ignoreCase = true) != true }
                .thenBy { it.serialNumber.lowercase() }
        )
}

data class IslandSearchCriteria(
    val textQuery: String? = null,
    val islandType: IslandType? = null,
    val facilityId: String? = null,
    val isActive: Boolean? = null,
    val underWarranty: Boolean? = null,
    val maintenanceOverdue: Boolean? = null,
    val operatingHoursMin: Int? = null,
    val operatingHoursMax: Int? = null,
    val cycleCountMin: Long? = null,
    val cycleCountMax: Long? = null
    // model field removed — use textQuery which searches modelNumber
)