package net.calvuz.qreport.domain.usecase.client.facilityisland

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.client.OperationalStatus
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import net.calvuz.qreport.domain.repository.CheckUpRepository
import javax.inject.Inject

/**
 * Use Case per statistiche di una singola isola robotizzata
 *
 * Questo use case è specifico per ottenere le statistiche
 * di un'isola individuale da mostrare nelle UI, card e dettagli.
 *
 * È diverso da GetAllFacilityIslandsStatisticsUseCase che gestisce
 * le statistiche aggregate per dashboard.
 */
class GetFacilityIslandStatisticsUseCase @Inject constructor(
    private val facilityIslandRepository: FacilityIslandRepository,
    private val checkUpRepository: CheckUpRepository? = null // Opzionale se non ancora disponibile
) {

    /**
     * Ottiene statistiche complete per una singola isola
     *
     * @param islandId ID dell'isola
     * @return Result con statistiche dell'isola per UI
     */
    suspend operator fun invoke(islandId: String): Result<SingleIslandStatistics> {
        return try {
            // Validazione input
            if (islandId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID isola non può essere vuoto"))
            }

            // Verifica esistenza isola
            val island = facilityIslandRepository.getIslandById(islandId)
                .getOrElse { return Result.failure(it) }
                ?: return Result.failure(NoSuchElementException("Isola non trovata"))

            val currentTime = Clock.System.now()

            // Calcola statistiche operative
            val operationalStats = calculateOperationalStats(island, currentTime)

            // Calcola stato manutenzione
            val maintenanceStats = calculateMaintenanceStats(island, currentTime)

            // Calcola stato garanzia
            val warrantyStats = calculateWarrantyStats(island, currentTime)

            // Statistiche CheckUp (opzionali se repository non disponibile)
            val (totalCheckUps, lastCheckUpDate, issuesCount) = if (checkUpRepository != null) {
                try {
                    // TODO: Implementare quando CheckUpRepository supporta ricerca per isola
                    // val checkUps = checkUpRepository.getCheckUpsByIsland(islandId)
                    //     .getOrElse { emptyList() }
                    //
                    // val lastDate = checkUps.maxByOrNull { it.updatedAt }?.updatedAt
                    // val issues = checkUps.sumOf { it.issuesCount }

                    Triple(0, null, 0)
                } catch (_: Exception) {
                    // Se fallisce, usa valori di default
                    Triple(0, null, 0)
                }
            } else {
                // Repository non disponibile, usa placeholder
                Triple(0, null, 0)
            }

            val stats = SingleIslandStatistics(
                islandId = island.id,
                serialNumber = island.serialNumber,
                islandType = island.islandType,
                operationalStats = operationalStats,
                maintenanceStats = maintenanceStats,
                warrantyStats = warrantyStats,
                totalCheckUps = totalCheckUps,
                lastCheckUpDate = lastCheckUpDate,
                issuesCount = issuesCount,
                generatedAt = currentTime
            )

            Result.success(stats)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene statistiche rapide senza CheckUp (versione veloce)
     *
     * Usa questa versione se le statistiche CheckUp non sono critiche
     * e vuoi performance migliori.
     */
    suspend fun getBasicStats(islandId: String): Result<SingleIslandStatistics> {
        return try {
            if (islandId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID isola non può essere vuoto"))
            }

            val island = facilityIslandRepository.getIslandById(islandId)
                .getOrElse { return Result.failure(it) }
                ?: return Result.failure(NoSuchElementException("Isola non trovata"))

            val currentTime = Clock.System.now()

            val stats = SingleIslandStatistics(
                islandId = island.id,
                serialNumber = island.serialNumber,
                islandType = island.islandType,
                operationalStats = calculateOperationalStats(island, currentTime),
                maintenanceStats = calculateMaintenanceStats(island, currentTime),
                warrantyStats = calculateWarrantyStats(island, currentTime),
                totalCheckUps = 0,        // Placeholder
                lastCheckUpDate = null,   // Placeholder
                issuesCount = 0,          // Placeholder
                generatedAt = currentTime
            )

            Result.success(stats)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calcola metriche operative
     */
    private fun calculateOperationalStats(island: FacilityIsland, currentTime: Instant): OperationalStats {
        val ageInDays = island.installationDate?.let { install ->
            ((currentTime - install).inWholeDays).toInt()
        } ?: 0

        val averageHoursPerDay = if (ageInDays > 0) {
            (island.operatingHours.toDouble() / ageInDays).toInt()
        } else 0

        val averageCyclesPerHour = if (island.operatingHours > 0) {
            (island.cycleCount.toDouble() / island.operatingHours).toInt()
        } else 0

        // Calcola uptime percentage approssimativo
        val expectedHoursPerDay = 16 // Assumendo 16h lavorative al giorno
        val expectedTotalHours = ageInDays * expectedHoursPerDay
        val uptime = if (expectedTotalHours > 0) {
            ((island.operatingHours.toDouble() / expectedTotalHours) * 100).coerceAtMost(100.0).toInt()
        } else 100

        // Performance score basato su cicli/ora
        val performanceScore = when (averageCyclesPerHour) {
            in 0..20 -> 20
            in 21..40 -> 40
            in 41..60 -> 60
            in 61..80 -> 80
            else -> 100
        }

        return OperationalStats(
            operatingHours = island.operatingHours,
            cycleCount = island.cycleCount,
            ageInDays = ageInDays,
            averageHoursPerDay = averageHoursPerDay,
            averageCyclesPerHour = averageCyclesPerHour,
            uptime = uptime,
            performanceScore = performanceScore,
            isActive = island.isActive
        )
    }

    /**
     * Calcola stato manutenzione
     */
    private fun calculateMaintenanceStats(island: FacilityIsland, currentTime: Instant): MaintenanceStats {
        val daysToNextMaintenance = island.daysToNextMaintenance()
        val isOverdue = island.needsMaintenance()

        val daysSinceLastMaintenance = island.lastMaintenanceDate?.let { lastDate ->
            ((currentTime - lastDate).inWholeDays).toInt()
        }

        val maintenanceStatus = when {
            isOverdue -> MaintenanceStatus.OVERDUE
            daysToNextMaintenance != null && daysToNextMaintenance <= 7 -> MaintenanceStatus.DUE_SOON
            daysToNextMaintenance != null && daysToNextMaintenance <= 30 -> MaintenanceStatus.SCHEDULED
            island.lastMaintenanceDate == null -> MaintenanceStatus.NO_HISTORY
            else -> MaintenanceStatus.UP_TO_DATE
        }

        return MaintenanceStats(
            status = maintenanceStatus,
            daysToNext = daysToNextMaintenance,
            daysSinceLast = daysSinceLastMaintenance,
            lastMaintenanceDate = island.lastMaintenanceDate,
            nextScheduledDate = island.nextScheduledMaintenance,
            statusText = island.maintenanceStatusText
        )
    }

    /**
     * Calcola stato garanzia
     */
    private fun calculateWarrantyStats(island: FacilityIsland, currentTime: Instant): WarrantyStats {
        val warrantyExpiration = island.warrantyExpiration

        val (status, daysRemaining) = when {
            warrantyExpiration == null -> WarrantyStatus.NO_INFO to null
            warrantyExpiration <= currentTime -> WarrantyStatus.EXPIRED to 0L
            else -> {
                val days = ((warrantyExpiration - currentTime).inWholeDays)
                when {
                    days <= 30 -> WarrantyStatus.EXPIRING_SOON to days
                    days <= 90 -> WarrantyStatus.EXPIRING_THIS_QUARTER to days
                    else -> WarrantyStatus.ACTIVE to days
                }
            }
        }

        return WarrantyStats(
            status = status,
            expirationDate = warrantyExpiration,
            daysRemaining = daysRemaining,
            isActive = status == WarrantyStatus.ACTIVE || status == WarrantyStatus.EXPIRING_SOON || status == WarrantyStatus.EXPIRING_THIS_QUARTER
        )
    }
}

/**
 * Statistiche complete per singola isola (per UI dettagli e card)
 */
data class SingleIslandStatistics(
    val islandId: String,
    val serialNumber: String,
    val islandType: net.calvuz.qreport.domain.model.island.IslandType,
    val operationalStats: OperationalStats,
    val maintenanceStats: MaintenanceStats,
    val warrantyStats: WarrantyStats,
    val totalCheckUps: Int,
    val lastCheckUpDate: Instant?,
    val issuesCount: Int,
    val generatedAt: Instant
) {

    /**
     * Score salute isola (0-100)
     */
    val healthScore: Int
        get() {
            var score = 0

            // Performance operativa (40%)
            score += (operationalStats.performanceScore * 0.4).toInt()

            // Stato manutenzione (35%)
            score += when (maintenanceStats.status) {
                MaintenanceStatus.UP_TO_DATE -> 35
                MaintenanceStatus.SCHEDULED -> 30
                MaintenanceStatus.DUE_SOON -> 15
                MaintenanceStatus.OVERDUE -> 0
                MaintenanceStatus.NO_HISTORY -> 20
            }

            // Stato garanzia (15%)
            score += when (warrantyStats.status) {
                WarrantyStatus.ACTIVE -> 15
                WarrantyStatus.EXPIRING_THIS_QUARTER -> 12
                WarrantyStatus.EXPIRING_SOON -> 8
                WarrantyStatus.EXPIRED -> 5
                WarrantyStatus.NO_INFO -> 7
            }

            // Assenza di problemi critici (10%)
            if (operationalStats.isActive) score += 10

            return score.coerceIn(0, 100)
        }

    /**
     * Stato operativo globale
     */
    val overallStatus: OperationalStatus
        get() = when {
            !operationalStats.isActive -> OperationalStatus.INACTIVE
            maintenanceStats.status == MaintenanceStatus.OVERDUE -> OperationalStatus.MAINTENANCE_DUE
            else -> OperationalStatus.OPERATIONAL
        }

    /**
     * Descrizione stato per UI
     */
    val statusDescription: String
        get() = when {
            !operationalStats.isActive -> "Non attiva"
            maintenanceStats.status == MaintenanceStatus.OVERDUE -> "Manutenzione scaduta"
            maintenanceStats.status == MaintenanceStatus.DUE_SOON -> "Manutenzione imminente"
            warrantyStats.status == WarrantyStatus.EXPIRING_SOON -> "Garanzia in scadenza"
            healthScore >= 80 -> "Operativa"
            healthScore >= 60 -> "Attenzione"
            else -> "Critica"
        }

    /**
     * Testo riassuntivo per UI
     */
    val summaryText: String
        get() = buildString {
            val parts = mutableListOf<String>()

            // Ore operative
            parts.add("${operationalStats.operatingHours}h operative")

            // Cicli
            when {
                operationalStats.cycleCount >= 1_000_000 -> parts.add("${(operationalStats.cycleCount / 1_000_000).toInt()}M cicli")
                operationalStats.cycleCount >= 1_000 -> parts.add("${(operationalStats.cycleCount / 1_000).toInt()}K cicli")
                else -> parts.add("${operationalStats.cycleCount} cicli")
            }

            // Età
            if (operationalStats.ageInDays > 0) {
                when {
                    operationalStats.ageInDays >= 365 -> parts.add("${operationalStats.ageInDays / 365} anni")
                    operationalStats.ageInDays >= 30 -> parts.add("${operationalStats.ageInDays / 30} mesi")
                    else -> parts.add("${operationalStats.ageInDays} giorni")
                }
            }

            append(parts.joinToString(" • "))
        }

    /**
     * Indica se richiede attenzione immediata
     */
    val needsAttention: Boolean
        get() = maintenanceStats.status == MaintenanceStatus.OVERDUE ||
                warrantyStats.status == WarrantyStatus.EXPIRING_SOON ||
                !operationalStats.isActive ||
                healthScore < 50
}

/**
 * Statistiche operative
 */
data class OperationalStats(
    val operatingHours: Int,
    val cycleCount: Long,
    val ageInDays: Int,
    val averageHoursPerDay: Int,
    val averageCyclesPerHour: Int,
    val uptime: Int, // Percentuale
    val performanceScore: Int, // 0-100
    val isActive: Boolean
)

/**
 * Statistiche manutenzione
 */
data class MaintenanceStats(
    val status: MaintenanceStatus,
    val daysToNext: Long?,
    val daysSinceLast: Int?,
    val lastMaintenanceDate: Instant?,
    val nextScheduledDate: Instant?,
    val statusText: String
)

/**
 * Statistiche garanzia
 */
data class WarrantyStats(
    val status: WarrantyStatus,
    val expirationDate: Instant?,
    val daysRemaining: Long?,
    val isActive: Boolean
)

/**
 * Stati manutenzione
 */
enum class MaintenanceStatus(val displayName: String) {
    UP_TO_DATE("Aggiornata"),
    SCHEDULED("Programmata"),
    DUE_SOON("Imminente"),
    OVERDUE("Scaduta"),
    NO_HISTORY("Nessuna cronologia")
}

/**
 * Stati garanzia
 */
enum class WarrantyStatus(val displayName: String) {
    ACTIVE("Attiva"),
    EXPIRING_THIS_QUARTER("In scadenza"),
    EXPIRING_SOON("Scade presto"),
    EXPIRED("Scaduta"),
    NO_INFO("Non specificata")
}