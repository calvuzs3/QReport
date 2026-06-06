package net.calvuz.qreport.client.island.maintenance.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.maintenance.data.local.entity.MaintenanceLogEntity
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOperationType
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOutcome

/**
 * Bidirectional mapper between [MaintenanceLogEntity] (Room) and [MaintenanceLog] (domain).
 *
 * Enum conversion uses safe fromName() companions — unknown stored names fall back
 * to OTHER / COMPLETED rather than throwing at runtime.
 *
 * Timestamp convention: domain uses [Instant], Room uses Long (epoch milliseconds).
 */

// ===== ENTITY → DOMAIN =====

fun MaintenanceLogEntity.toDomain(): MaintenanceLog = MaintenanceLog(
    id = id,
    islandId = islandId,
    operationType = MaintenanceOperationType.fromName(operationType),
    customOperationLabel = customOperationLabel,
    mechanicalUnitId = mechanicalUnitId,
    componentLabel = componentLabel,
    description = description,
    technicianName = technicianName,
    technicianCompany = technicianCompany,
    operatingHoursAtEvent = operatingHoursAtEvent,
    cycleCountAtEvent = cycleCountAtEvent,
    outcome = MaintenanceOutcome.fromName(outcome),
    durationMinutes = durationMinutes,
    notes = notes,
    performedAt = Instant.fromEpochMilliseconds(performedAt),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    isActive = isActive,
    isDeleted = isDeleted
)

fun List<MaintenanceLogEntity>.toDomain(): List<MaintenanceLog> = map { it.toDomain() }

// ===== DOMAIN → ENTITY =====

fun MaintenanceLog.toEntity(): MaintenanceLogEntity = MaintenanceLogEntity(
    id = id,
    islandId = islandId,
    operationType = operationType.name,
    customOperationLabel = customOperationLabel,
    mechanicalUnitId = mechanicalUnitId,
    componentLabel = componentLabel,
    description = description,
    technicianName = technicianName,
    technicianCompany = technicianCompany,
    operatingHoursAtEvent = operatingHoursAtEvent,
    cycleCountAtEvent = cycleCountAtEvent,
    outcome = outcome.name,
    durationMinutes = durationMinutes,
    notes = notes,
    performedAt = performedAt.toEpochMilliseconds(),
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    isActive = isActive,
    isDeleted = isDeleted
)