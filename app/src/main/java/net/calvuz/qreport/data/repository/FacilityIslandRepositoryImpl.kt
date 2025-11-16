package net.calvuz.qreport.data.repository

import net.calvuz.qreport.data.local.dao.FacilityIslandDao
import net.calvuz.qreport.data.mapper.FacilityIslandMapper
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.domain.model.island.IslandType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Implementazione del repository per gestione isole robotizzate per stabilimenti
 * Utilizza Room DAO per persistenza e mapper per conversioni domain ↔ entity
 *
 * ✅ FIXED: Tutti i metodi corretti per compilazione
 * ✅ FIXED: FacilityIslandMapper integrato
 * ✅ FIXED: Conversioni Instant/Long corrette
 * ✅ FIXED: Flow methods senza suspend
 */
class FacilityIslandRepositoryImpl @Inject constructor(
    private val facilityIslandDao: FacilityIslandDao,
    private val facilityIslandMapper: FacilityIslandMapper
) : FacilityIslandRepository {

    // ===== CRUD OPERATIONS =====

    override suspend fun getAllIslands(): Result<List<FacilityIsland>> {
        return try {
            val entities = facilityIslandDao.getAllActiveIslands()
            val islands = facilityIslandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveIslands(): Result<List<FacilityIsland>> {
        return try {
            val entities = facilityIslandDao.getAllActiveIslands()
            val islands = facilityIslandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandById(id: String): Result<FacilityIsland?> {
        return try {
            val entity = facilityIslandDao.getIslandById(id)
            val island = entity?.let { facilityIslandMapper.toDomain(it) }
            Result.success(island)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createIsland(island: FacilityIsland): Result<Unit> {
        return try {
            val entity = facilityIslandMapper.toEntity(island)
            facilityIslandDao.insertIsland(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateIsland(island: FacilityIsland): Result<Unit> {
        return try {
            val entity = facilityIslandMapper.toEntity(island)
            facilityIslandDao.updateIsland(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteIsland(id: String): Result<Unit> {
        return try {
            facilityIslandDao.softDeleteIsland(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== FACILITY RELATED =====

    override suspend fun getIslandsByFacility(facilityId: String): Result<List<FacilityIsland>> {
        return try {
            val entities = facilityIslandDao.getIslandsForFacility(facilityId)
            val islands = facilityIslandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getIslandsByFacilityFlow(facilityId: String): Flow<List<FacilityIsland>> {
        return facilityIslandDao.getIslandsForFacilityFlow(facilityId).map { entities ->
            facilityIslandMapper.toDomainList(entities)
        }
    }

    override suspend fun getActiveIslandsByFacility(facilityId: String): Result<List<FacilityIsland>> {
        return try {
            val entities = facilityIslandDao.getIslandsForFacility(facilityId) // Già filtrato per is_active
            val islands = facilityIslandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== SEARCH & FILTER =====

    override suspend fun getIslandsByType(islandType: IslandType): Result<List<FacilityIsland>> {
        return try {
            val entities = facilityIslandDao.getIslandsByType(islandType.name)
            val islands = facilityIslandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandBySerialNumber(serialNumber: String): Result<FacilityIsland?> {
        return try {
            val entity = facilityIslandDao.getIslandBySerialNumber(serialNumber)
            val island = entity?.let { facilityIslandMapper.toDomain(it) }
            Result.success(island)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchIslands(query: String): Result<List<FacilityIsland>> {
        return try {
            val entities = facilityIslandDao.searchIslands(query)
            val islands = facilityIslandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== FLOW OPERATIONS (REACTIVE) =====
    // ✅ FIXED: Rimosso suspend da metodi Flow

    override fun getAllActiveIslandsFlow(): Flow<List<FacilityIsland>> {
        return facilityIslandDao.getAllActiveIslandsFlow().map { entities ->
            facilityIslandMapper.toDomainList(entities)
        }
    }

    override fun getIslandByIdFlow(id: String): Flow<FacilityIsland?> {
        return facilityIslandDao.getIslandByIdFlow(id).map { entity ->
            entity?.let { facilityIslandMapper.toDomain(it) }
        }
    }

    // ===== VALIDATION =====

    override suspend fun isSerialNumberTaken(serialNumber: String, excludeId: String): Result<Boolean> {
        return try {
            val isTaken = facilityIslandDao.isSerialNumberTaken(serialNumber, excludeId)
            Result.success(isTaken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== MAINTENANCE OPERATIONS =====

    override suspend fun getIslandsRequiringMaintenance(currentTime: Instant?): Result<List<FacilityIsland>> {
        return try {
            val timestamp = currentTime?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds()
            val entities = facilityIslandDao.getIslandsDueMaintenance(timestamp)
            val islands = facilityIslandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsUnderWarranty(currentTime: Instant?): Result<List<FacilityIsland>> {
        return try {
            val timestamp = currentTime?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds()
            val entities = facilityIslandDao.getIslandsUnderWarranty(timestamp)
            val islands = facilityIslandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMaintenanceDate(islandId: String, maintenanceDate: Instant): Result<Unit> {
        return try {
            facilityIslandDao.updateLastMaintenanceDate(islandId, maintenanceDate.toEpochMilliseconds())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOperatingHours(islandId: String, operatingHours: Int): Result<Unit> {
        return try {
            facilityIslandDao.updateOperatingHours(islandId, operatingHours)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCycleCount(islandId: String, cycleCount: Long): Result<Unit> {
        return try {
            facilityIslandDao.updateCycleCount(islandId, cycleCount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== STATISTICS =====

    override suspend fun getActiveIslandsCount(): Result<Int> {
        return try {
            val count = facilityIslandDao.getActiveIslandsCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsCountByFacility(facilityId: String): Result<Int> {
        return try {
            val count = facilityIslandDao.getIslandsCountForFacility(facilityId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsCountByType(islandType: IslandType): Result<Int> {
        return try {
            val count = facilityIslandDao.getIslandsCountByType(islandType.name)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandTypeStats(): Result<Map<IslandType, Int>> {
        return try {
            val stats = facilityIslandDao.getIslandTypeStatistics()
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
            val dueMaintenance = facilityIslandDao.getIslandsDueMaintenance(currentTime)
            val underWarranty = facilityIslandDao.getIslandsUnderWarranty(currentTime)

            val stats = mapOf(
                "due_maintenance" to dueMaintenance.size,
                "under_warranty" to underWarranty.size,
                "total_active" to facilityIslandDao.getActiveIslandsCount()
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== CLIENT AGGREGATION =====

    override suspend fun getIslandsByClient(clientId: String): Result<List<FacilityIsland>> {
        return try {
            val entities = facilityIslandDao.getIslandsForClient(clientId)
            val islands = facilityIslandMapper.toDomainList(entities)
            Result.success(islands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsCountByClient(clientId: String): Result<Int> {
        return try {
            val count = facilityIslandDao.getIslandsCountForClient(clientId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== BULK OPERATIONS =====

    override suspend fun createIslands(islands: List<FacilityIsland>): Result<Unit> {
        return try {
            val entities = facilityIslandMapper.toEntityList(islands)
            facilityIslandDao.insertIslands(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkUpdateMaintenanceDates(updates: Map<String, Instant>): Result<Unit> {
        return try {
            updates.forEach { (islandId, maintenanceDate) ->
                facilityIslandDao.updateLastMaintenanceDate(islandId, maintenanceDate.toEpochMilliseconds())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== MAINTENANCE =====

    override suspend fun touchIsland(id: String): Result<Unit> {
        return try {
            facilityIslandDao.touchIsland(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}