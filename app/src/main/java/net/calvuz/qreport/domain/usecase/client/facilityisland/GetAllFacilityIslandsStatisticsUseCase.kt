package net.calvuz.qreport.domain.usecase.client.facilityisland

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Use Case per statistiche e analytics isole robotizzate
 *
 * Gestisce:
 * - Dashboard executive per isole
 * - Metriche operative aggregate
 * - Analisi per tipo isola
 * - KPI manutenzione e garanzie
 * - Performance tracking
 */
class GetAllFacilityIslandsStatisticsUseCase @Inject constructor(
    private val facilityIslandRepository: FacilityIslandRepository
) {

    /**
     * Ottiene statistiche complete per dashboard
     *
     * @return Result con oggetto statistiche complete
     */
    suspend operator fun invoke(): Result<IslandStatistics> {
        return try {
            val activeCount = facilityIslandRepository.getActiveIslandsCount()
                .getOrElse { return Result.failure(it) }

            val typeStats = facilityIslandRepository.getIslandTypeStats()
                .getOrElse { return Result.failure(it) }

            val maintenanceStats = facilityIslandRepository.getMaintenanceStats()
                .getOrElse { return Result.failure(it) }

            val allIslands = facilityIslandRepository.getActiveIslands()
                .getOrElse { return Result.failure(it) }

            val currentTime = Clock.System.now()

            // Calcola metriche operative
            val operationalMetrics = calculateOperationalMetrics(allIslands)

            // Calcola metriche manutenzione
            val maintenanceMetrics = calculateMaintenanceMetrics(allIslands, currentTime)

            // Calcola metriche garanzia
            val warrantyMetrics = calculateWarrantyMetrics(allIslands, currentTime)

            val statistics = IslandStatistics(
                totalActiveIslands = activeCount,
                islandsByType = typeStats,
                operationalMetrics = operationalMetrics,
                maintenanceMetrics = maintenanceMetrics,
                warrantyMetrics = warrantyMetrics,
                systemHealth = calculateSystemHealth(maintenanceMetrics, warrantyMetrics),
                generatedAt = currentTime
            )

            Result.success(statistics)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene statistiche dettagliate per un tipo di isola
     *
     * @param islandType Tipo di isola
     * @return Result con statistiche specifiche del tipo
     */
    suspend fun getTypeSpecificStatistics(islandType: IslandType): Result<TypeSpecificStatistics> {
        return try {
            val typeIslands = facilityIslandRepository.getIslandsByType(islandType)
                .getOrElse { return Result.failure(it) }

            val activeIslands = typeIslands.filter { it.isActive }

            if (activeIslands.isEmpty()) {
                return Result.success(
                    TypeSpecificStatistics(
                        islandType = islandType,
                        totalCount = 0,
                        averageOperatingHours = 0,
                        averageCycleCount = 0L,
                        totalOperatingHours = 0,
                        totalCycleCount = 0L,
                        underWarrantyCount = 0,
                        maintenanceOverdueCount = 0,
                        averageAge = 0,
                        performanceScore = 0
                    )
                )
            }

            val currentTime = Clock.System.now()

            val stats = TypeSpecificStatistics(
                islandType = islandType,
                totalCount = activeIslands.size,
                averageOperatingHours = activeIslands.map { it.operatingHours }.average().toInt(),
                averageCycleCount = activeIslands.map { it.cycleCount }.average().toLong(),
                totalOperatingHours = activeIslands.sumOf { it.operatingHours },
                totalCycleCount = activeIslands.sumOf { it.cycleCount },
                underWarrantyCount = activeIslands.count { island ->
                    island.warrantyExpiration?.let { it > currentTime } == true
                },
                maintenanceOverdueCount = activeIslands.count { island ->
                    island.nextScheduledMaintenance?.let { it <= currentTime } == true
                },
                averageAge = calculateAverageAge(activeIslands, currentTime),
                performanceScore = calculatePerformanceScore(activeIslands, currentTime)
            )

            Result.success(stats)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene top N isole per performance
     *
     * @param limit Numero massimo di isole da restituire
     * @param metric Metrica per ordinamento (OPERATING_HOURS, CYCLE_COUNT, UPTIME)
     * @return Result con lista isole top performer
     */
    suspend fun getTopPerformingIslands(
        limit: Int = 10,
        metric: PerformanceMetric = PerformanceMetric.OPERATING_HOURS
    ): Result<List<IslandPerformance>> {
        return try {
            val allIslands = facilityIslandRepository.getActiveIslands()
                .getOrElse { return Result.failure(it) }

            val currentTime = Clock.System.now()

            val performances = allIslands.map { island ->
                val uptime = calculateUptime(island, currentTime)
                val efficiency = calculateEfficiency(island)

                IslandPerformance(
                    islandId = island.id,
                    serialNumber = island.serialNumber,
                    islandType = island.islandType,
                    customName = island.customName,
                    operatingHours = island.operatingHours,
                    cycleCount = island.cycleCount,
                    uptime = uptime,
                    efficiency = efficiency,
                    performanceScore = (uptime * 0.4 + efficiency * 0.6).toInt()
                )
            }

            val sortedPerformances = when (metric) {
                PerformanceMetric.OPERATING_HOURS ->
                    performances.sortedByDescending { it.operatingHours }
                PerformanceMetric.CYCLE_COUNT ->
                    performances.sortedByDescending { it.cycleCount }
                PerformanceMetric.UPTIME ->
                    performances.sortedByDescending { it.uptime }
                PerformanceMetric.EFFICIENCY ->
                    performances.sortedByDescending { it.efficiency }
                PerformanceMetric.PERFORMANCE_SCORE ->
                    performances.sortedByDescending { it.performanceScore }
            }

            Result.success(sortedPerformances.take(limit))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene trend di manutenzione per analisi predittiva
     *
     * @param daysAhead Giorni nel futuro da analizzare
     * @return Result con analisi trend manutenzione
     */
    suspend fun getMaintenanceTrend(daysAhead: Int = 90): Result<MaintenanceTrend> {
        return try {
            val allIslands = facilityIslandRepository.getActiveIslands()
                .getOrElse { return Result.failure(it) }

            val currentTime = Clock.System.now()
            val endTime = currentTime + (daysAhead.days)

            val scheduledMaintenances = allIslands.mapNotNull { island ->
                island.nextScheduledMaintenance?.let { nextMaintenance ->
                    if (nextMaintenance >= currentTime && nextMaintenance <= endTime) {
                        Pair(island, nextMaintenance)
                    } else null
                }
            }

            // Raggruppa per settimane
            val weeklyMaintenances = mutableMapOf<Int, Int>()
            scheduledMaintenances.forEach { (_, maintenanceDate) ->
                val weekNumber = ((maintenanceDate - currentTime) / (7.days)).toInt()
                weeklyMaintenances[weekNumber] = weeklyMaintenances.getOrDefault(weekNumber, 0) + 1
            }

            val trend = MaintenanceTrend(
                totalScheduled = scheduledMaintenances.size,
                weeklyDistribution = weeklyMaintenances.toMap(),
                peakWeek = weeklyMaintenances.maxByOrNull { it.value }?.key ?: 0,
                averagePerWeek = if (daysAhead >= 7) scheduledMaintenances.size / (daysAhead / 7) else 0,
                daysAnalyzed = daysAhead
            )

            Result.success(trend)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calcola metriche operative aggregate
     */
    private fun calculateOperationalMetrics(
        islands: List<FacilityIsland>
    ): OperationalMetrics {
        if (islands.isEmpty()) {
            return OperationalMetrics(0, 0, 0L, 0L, 0, 0, 0.0)
        }

        val totalHours = islands.sumOf { it.operatingHours }
        val totalCycles = islands.sumOf { it.cycleCount }
        val avgHours = islands.map { it.operatingHours }.average().toInt()
        val avgCycles = islands.map { it.cycleCount }.average().toLong()
        val maxHours = islands.maxOf { it.operatingHours }
        val maxCycles = islands.maxOf { it.cycleCount }
        val avgEfficiency = islands.map { calculateEfficiency(it) }.average()

        return OperationalMetrics(
            totalOperatingHours = totalHours,
            averageOperatingHours = avgHours,
            totalCycleCount = totalCycles,
            averageCycleCount = avgCycles,
            maxOperatingHours = maxHours,
            maxCycleCount = maxCycles,
            averageEfficiency = avgEfficiency
        )
    }

    /**
     * Calcola metriche di manutenzione
     */
    private fun calculateMaintenanceMetrics(
        islands: List<FacilityIsland>,
        currentTime: Instant
    ): MaintenanceMetrics {
        val totalIslands = islands.size
        val overdueCount = islands.count { island ->
            island.nextScheduledMaintenance?.let { it <= currentTime } == true
        }
        val upToDateCount = totalIslands - overdueCount

        val upToDatePercentage = if (totalIslands > 0) {
            (upToDateCount.toDouble() / totalIslands * 100).toInt()
        } else 100

        return MaintenanceMetrics(
            totalIslands = totalIslands,
            upToDate = upToDateCount,
            overdue = overdueCount,
            upToDatePercentage = upToDatePercentage
        )
    }

    /**
     * Calcola metriche di garanzia
     */
    private fun calculateWarrantyMetrics(
        islands: List<FacilityIsland>,
        currentTime: Instant
    ): WarrantyMetrics {
        val totalIslands = islands.size
        val underWarranty = islands.count { island ->
            island.warrantyExpiration?.let { it > currentTime } == true
        }
        val expiredWarranty = islands.count { island ->
            island.warrantyExpiration?.let { it <= currentTime } == true
        }
        val noWarrantyInfo = totalIslands - underWarranty - expiredWarranty

        val underWarrantyPercentage = if (totalIslands > 0) {
            (underWarranty.toDouble() / totalIslands * 100).toInt()
        } else 0

        return WarrantyMetrics(
            totalIslands = totalIslands,
            underWarranty = underWarranty,
            expired = expiredWarranty,
            noInformation = noWarrantyInfo,
            underWarrantyPercentage = underWarrantyPercentage
        )
    }

    /**
     * Calcola score di salute del sistema
     */
    private fun calculateSystemHealth(
        maintenanceMetrics: MaintenanceMetrics,
        warrantyMetrics: WarrantyMetrics
    ): Int {
        val maintenanceScore = maintenanceMetrics.upToDatePercentage
        val warrantyScore = warrantyMetrics.underWarrantyPercentage

        // Peso maggiore alla manutenzione per la salute del sistema
        return ((maintenanceScore * 0.7) + (warrantyScore * 0.3)).toInt()
    }

    /**
     * Calcola età media in giorni
     */
    private fun calculateAverageAge(
        islands: List<FacilityIsland>,
        currentTime: Instant
    ): Int {
        if (islands.isEmpty()) return 0

        val ages = islands.mapNotNull { island ->
            island.installationDate?.let { installation ->
                ((currentTime - installation).inWholeDays)
            }
        }

        return if (ages.isNotEmpty()) ages.average().toInt() else 0
    }

    /**
     * Calcola score di performance per tipo
     */
    private fun calculatePerformanceScore(
        islands: List<FacilityIsland>,
        currentTime: Instant
    ): Int {
        if (islands.isEmpty()) return 0

        val scores = islands.map { island ->
            val uptime = calculateUptime(island, currentTime)
            val efficiency = calculateEfficiency(island)
            (uptime * 0.5 + efficiency * 0.5).toInt()
        }

        return scores.average().toInt()
    }

    /**
     * Calcola uptime percentage
     */
    private fun calculateUptime(
        island: FacilityIsland,
        currentTime: Instant
    ): Int {
        val installationDate = island.installationDate ?: currentTime
        val totalDays = ((currentTime - installationDate).inWholeDays).toInt()
        val operatingDays = (island.operatingHours / 24).coerceAtMost(totalDays)

        return if (totalDays > 0) {
            (operatingDays.toDouble() / totalDays * 100).toInt()
        } else 100
    }

    /**
     * Calcola efficienza relativa
     */
    private fun calculateEfficiency(island: FacilityIsland): Int {
        // Efficienza basata su cicli per ora operativa
        return if (island.operatingHours > 0) {
            val cyclesPerHour = island.cycleCount.toDouble() / island.operatingHours
            // Normalizza basandosi su medie di settore (assumendo 50-100 cicli/ora come range normale)
            ((cyclesPerHour / 100.0) * 100).coerceAtMost(100.0).toInt()
        } else 0
    }
}

/**
 * Statistiche complete isole
 */
data class IslandStatistics(
    val totalActiveIslands: Int,
    val islandsByType: Map<IslandType, Int>,
    val operationalMetrics: OperationalMetrics,
    val maintenanceMetrics: MaintenanceMetrics,
    val warrantyMetrics: WarrantyMetrics,
    val systemHealth: Int, // 0-100
    val generatedAt: Instant
)

/**
 * Metriche operative aggregate
 */
data class OperationalMetrics(
    val totalOperatingHours: Int,
    val averageOperatingHours: Int,
    val totalCycleCount: Long,
    val averageCycleCount: Long,
    val maxOperatingHours: Int,
    val maxCycleCount: Long,
    val averageEfficiency: Double
)

/**
 * Metriche manutenzione
 */
data class MaintenanceMetrics(
    val totalIslands: Int,
    val upToDate: Int,
    val overdue: Int,
    val upToDatePercentage: Int
)

/**
 * Metriche garanzia
 */
data class WarrantyMetrics(
    val totalIslands: Int,
    val underWarranty: Int,
    val expired: Int,
    val noInformation: Int,
    val underWarrantyPercentage: Int
)

/**
 * Statistiche specifiche per tipo
 */
data class TypeSpecificStatistics(
    val islandType: IslandType,
    val totalCount: Int,
    val averageOperatingHours: Int,
    val averageCycleCount: Long,
    val totalOperatingHours: Int,
    val totalCycleCount: Long,
    val underWarrantyCount: Int,
    val maintenanceOverdueCount: Int,
    val averageAge: Int, // in giorni
    val performanceScore: Int // 0-100
)

/**
 * Performance di una singola isola
 */
data class IslandPerformance(
    val islandId: String,
    val serialNumber: String,
    val islandType: IslandType,
    val customName: String?,
    val operatingHours: Int,
    val cycleCount: Long,
    val uptime: Int, // Percentage
    val efficiency: Int, // Percentage
    val performanceScore: Int // 0-100
)

/**
 * Trend manutenzione
 */
data class MaintenanceTrend(
    val totalScheduled: Int,
    val weeklyDistribution: Map<Int, Int>, // Settimana -> Numero manutenzioni
    val peakWeek: Int,
    val averagePerWeek: Int,
    val daysAnalyzed: Int
)

/**
 * Enum per metriche di performance
 */
enum class PerformanceMetric {
    OPERATING_HOURS,
    CYCLE_COUNT,
    UPTIME,
    EFFICIENCY,
    PERFORMANCE_SCORE
}