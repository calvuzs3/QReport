package net.calvuz.qreport.client.island.maintenance.data.local.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.client.island.data.local.dao.IslandDao
import net.calvuz.qreport.client.island.maintenance.data.local.dao.MaintenanceLogDao
import net.calvuz.qreport.client.island.maintenance.data.local.dao.OperationTypeCount
import net.calvuz.qreport.client.island.maintenance.data.local.mapper.toDomain
import net.calvuz.qreport.client.island.maintenance.data.local.mapper.toEntity
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceLog
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOperationType
import net.calvuz.qreport.client.island.maintenance.domain.repository.MaintenanceLogRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceLogRepositoryImpl @Inject constructor(
    private val dao: MaintenanceLogDao,
    private val islandDao: IslandDao,
    private val database: QReportDatabase
) : MaintenanceLogRepository {

    // ===== REACTIVE =====

    override fun getLogsForIslandFlow(islandId: String): Flow<List<MaintenanceLog>> =
        dao.getLogsForIslandFlow(islandId).map { it.toDomain() }

    override fun getLogsForUnitFlow(unitId: String): Flow<List<MaintenanceLog>> =
        dao.getLogsForUnitFlow(unitId).map { it.toDomain() }

    // ===== SUSPEND — SINGLE =====

    override suspend fun getLogById(id: String): Result<MaintenanceLog?> =
        runCatching { dao.getLogById(id)?.toDomain() }

    // ===== SUSPEND — LIST =====

    override suspend fun getLogsForIsland(islandId: String): Result<List<MaintenanceLog>> =
        runCatching { dao.getLogsForIsland(islandId).toDomain() }

    override suspend fun getRecentLogsForIsland(
        islandId: String,
        limit: Int
    ): Result<List<MaintenanceLog>> =
        runCatching { dao.getRecentLogsForIsland(islandId, limit).toDomain() }

    // ===== SUSPEND — AGGREGATE =====

    override suspend fun countLogsForIsland(islandId: String): Result<Int> =
        runCatching { dao.countLogsForIsland(islandId) }

    override suspend fun countEmergencyLogsForIsland(islandId: String): Result<Int> =
        runCatching { dao.countEmergencyLogsForIsland(islandId) }

    override suspend fun countDeferredLogsForIsland(islandId: String): Result<Int> =
        runCatching { dao.countDeferredLogsForIsland(islandId) }

    override suspend fun avgDurationForIsland(islandId: String): Result<Double?> =
        runCatching { dao.avgDurationForIsland(islandId) }

    override suspend fun lastPerformedAtForIsland(islandId: String): Result<Instant?> =
        runCatching {
            dao.lastPerformedAtForIsland(islandId)
                ?.let { Instant.fromEpochMilliseconds(it) }
        }

    override suspend fun allPerformedAtForIsland(islandId: String): Result<List<Instant>> =
        runCatching {
            dao.allPerformedAtForIsland(islandId)
                .map { Instant.fromEpochMilliseconds(it) }
        }

    override suspend fun operationTypeCountsForIsland(
        islandId: String
    ): Result<Map<MaintenanceOperationType, Int>> =
        runCatching {
            dao.operationTypeCountsForIsland(islandId).toOperationTypeMap()
        }

    override suspend fun recurrentComponentsForIsland(
        islandId: String,
        since: Instant
    ): Result<List<String>> =
        runCatching {
            val sinceMs = since.toEpochMilliseconds()
            val unitIds = dao.recurrentUnitIdsForIsland(islandId, sinceMs)
                .map { it.componentKey }
            val labels = dao.recurrentComponentLabelsForIsland(islandId, sinceMs)
                .map { it.componentKey }
            unitIds + labels
        }

    override suspend fun lastRevampingAtForIsland(islandId: String): Result<Instant?> =
        runCatching {
            dao.lastRevampingAtForIsland(islandId)
                ?.let { Instant.fromEpochMilliseconds(it) }
        }

    override suspend fun countEmergenciesAfter(islandId: String, since: Instant): Result<Int> =
        runCatching { dao.countEmergenciesAfter(islandId, since.toEpochMilliseconds()) }

    // ===== WRITE =====

    override suspend fun createLog(log: MaintenanceLog): Result<Unit> =
        runCatching { dao.insertLog(log.toEntity()) }

    // ===== LIFECYCLE =====

    override suspend fun deactivateLog(id: String, ts: Long): Result<Unit> =
        runCatching {
            dao.deactivateLog(id, ts)
        }

    override suspend fun markLogDeleted(id: String, ts: Long): Result<Unit> =
        runCatching {
            dao.markLogDeleted(id, ts)
        }

    // ===== RESTORE =====

    @Suppress("HardCodedStringLiteral")
    override suspend fun restoreLog(id: String, ts: Long): Result<Unit> = runCatching {
        database.withTransaction {
            val log = dao.getLogById(id) ?: error("Log not found: $id")
            val island = islandDao.getIslandById(log.islandId) ?: error("Island not found: ${log.islandId}")

            dao.restoreLog(id, ts)
            islandDao.restoreIsland(island.id, ts)
        }
    }

    // ===== BACKUP =====

    override suspend fun getAllForBackup(): Result<List<MaintenanceLog>> =
        runCatching { dao.getAllForBackup().toDomain() }

    override suspend fun insertAllFromBackup(logs: List<MaintenanceLog>): Result<Unit> =
        runCatching { dao.insertAllFromBackup(logs.map { it.toEntity() }) }

    // ===== PRIVATE HELPERS =====

    private fun List<OperationTypeCount>.toOperationTypeMap(): Map<MaintenanceOperationType, Int> =
        associate { MaintenanceOperationType.fromName(it.operation_type) to it.count }
}