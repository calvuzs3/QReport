package net.calvuz.qreport.client.island.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.client.island.data.local.dao.IslandDao
import net.calvuz.qreport.client.island.data.local.mapper.IslandMapper
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import javax.inject.Inject

/**
 * Implementazione del repository per gestione isole robotizzate per stabilimenti
 * Utilizza Room DAO per persistenza e mapper per conversioni domain ↔ entity
 *
 * ✅ FIXED: Tutti i metodi corretti per compilazione
 * ✅ FIXED: IslandMapper integrato
 * ✅ FIXED: Conversioni Instant/Long corrette
 * ✅ FIXED: Flow methods senza suspend
 */
class IslandRepositoryImpl @Inject constructor(
    private val islandDao: IslandDao,
    private val islandMapper: IslandMapper
) : IslandRepository {

    // ===== CRUD OPERATIONS =====

    override suspend fun getAllIslands(): Result<List<Island>> {
        return try {
            val entities = islandDao.getAllActiveIslands()
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveIslands(): Result<List<Island>> {
        return try {
            val entities = islandDao.getAllActiveIslands()
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandById(id: String): Result<Island?> {
        return try {
            val entity = islandDao.getIslandById(id)
            val island = entity?.let { islandMapper.toDomain(it) }
            Result.success(island)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsByIds(ids: List<String>): Result<List<Island>> {
        return try {
            if (ids.isEmpty()) {
                return Result.success(emptyList())
            }

            val entities = islandDao.getIslandsByIds(ids)
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createIsland(island: Island): Result<Unit> {
        return try {
            val entity = islandMapper.toEntity(island)
            islandDao.insertIsland(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateIsland(island: Island): Result<Unit> {
        return try {
            val entity = islandMapper.toEntity(island)
            islandDao.updateIsland(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteIsland(id: String): Result<Unit> {
        return try {
            islandDao.softDeleteIsland(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== FACILITY RELATED =====

    override suspend fun getIslandsByFacility(facilityId: String): Result<List<Island>> {
        return try {
            val entities = islandDao.getIslandsForFacility(facilityId)
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getIslandsByFacilityFlow(facilityId: String): Flow<List<Island>> {
        return islandDao.getIslandsForFacilityFlow(facilityId).map { entities ->
            islandMapper.toDomainList(entities)
        }
    }

    override suspend fun getActiveIslandsByFacility(facilityId: String): Result<List<Island>> {
        return try {
            val entities = islandDao.getIslandsForFacility(facilityId) // Già filtrato per is_active
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== SEARCH & FILTER =====

    override suspend fun getIslandsByType(islandType: IslandType): Result<List<Island>> {
        return try {
            val entities = islandDao.getIslandsByType(islandType.name)
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandBySerialNumber(serialNumber: String): Result<Island?> {
        return try {
            val entity = islandDao.getIslandBySerialNumber(serialNumber)
            val island = entity?.let { islandMapper.toDomain(it) }
            Result.success(island)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchIslands(query: String): Result<List<Island>> {
        return try {
            val entities = islandDao.searchIslands(query)
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== FLOW OPERATIONS (REACTIVE) =====
    // ✅ FIXED: Rimosso suspend da metodi Flow

    override fun getAllActiveIslandsFlow(): Flow<List<Island>> {
        return islandDao.getAllActiveIslandsFlow().map { entities ->
            islandMapper.toDomainList(entities)
        }
    }

    override fun getIslandByIdFlow(id: String): Flow<Island?> {
        return islandDao.getIslandByIdFlow(id).map { entity ->
            entity?.let { islandMapper.toDomain(it) }
        }
    }

    // ===== VALIDATION =====

    override suspend fun isSerialNumberTaken(serialNumber: String, excludeId: String): Result<Boolean> {
        return try {
            val isTaken = islandDao.isSerialNumberTaken(serialNumber, excludeId)
            Result.success(isTaken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== MAINTENANCE OPERATIONS =====

    override suspend fun getIslandsRequiringMaintenance(currentTime: Instant?): Result<List<Island>> {
        return try {
            val timestamp = currentTime?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds()
            val entities = islandDao.getIslandsDueMaintenance(timestamp)
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsUnderWarranty(currentTime: Instant?): Result<List<Island>> {
        return try {
            val timestamp = currentTime?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds()
            val entities = islandDao.getIslandsUnderWarranty(timestamp)
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMaintenanceDate(islandId: String, maintenanceDate: Instant): Result<Unit> {
        return try {
            islandDao.updateLastMaintenanceDate(islandId, maintenanceDate.toEpochMilliseconds())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOperatingHours(islandId: String, operatingHours: Int): Result<Unit> {
        return try {
            islandDao.updateOperatingHours(islandId, operatingHours)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCycleCount(islandId: String, cycleCount: Long): Result<Unit> {
        return try {
            islandDao.updateCycleCount(islandId, cycleCount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== STATISTICS =====

    override suspend fun getActiveIslandsCount(): Result<Int> {
        return try {
            val count = islandDao.getActiveIslandsCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsCountByFacility(facilityId: String): Result<Int> {
        return try {
            val count = islandDao.getIslandsCountForFacility(facilityId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsCountByType(islandType: IslandType): Result<Int> {
        return try {
            val count = islandDao.getIslandsCountByType(islandType.name)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandTypeStats(): Result<Map<IslandType, Int>> {
        return try {
            val stats = islandDao.getIslandTypeStatistics()
            val islandTypeStats = stats.mapNotNull { stat ->
                try {
                    IslandType.valueOf(stat.island_type) to stat.count
                } catch (_: IllegalArgumentException) {
                    null // Ignora tipi non validi
                }
            }.toMap()
            Result.success(islandTypeStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMaintenanceStats(): Result<Map<String, Int>> {
        return try {
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val dueMaintenance = islandDao.getIslandsDueMaintenance(currentTime)
            val underWarranty = islandDao.getIslandsUnderWarranty(currentTime)

            val stats = mapOf(
                "due_maintenance" to dueMaintenance.size,
                "under_warranty" to underWarranty.size,
                "total_active" to islandDao.getActiveIslandsCount()
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== CLIENT AGGREGATION =====

    override suspend fun getIslandsByClient(clientId: String): Result<List<Island>> {
        return try {
            val entities = islandDao.getIslandsForClient(clientId)
            val islands = islandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsCountByClient(clientId: String): Result<Int> {
        return try {
            val count = islandDao.getIslandsCountForClient(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== BULK OPERATIONS =====

    override suspend fun createIslands(islands: List<Island>): Result<Unit> {
        return try {
            val entities = islandMapper.toEntityList(islands)
            islandDao.insertIslands(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkUpdateMaintenanceDates(updates: Map<String, Instant>): Result<Unit> {
        return try {
            updates.forEach { (islandId, maintenanceDate) ->
                islandDao.updateLastMaintenanceDate(islandId, maintenanceDate.toEpochMilliseconds())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== MAINTENANCE =====

    override suspend fun touchIsland(id: String): Result<Unit> {
        return try {
            islandDao.touchIsland(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}