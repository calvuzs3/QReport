package net.calvuz.qreport.client.unit.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import net.calvuz.qreport.client.unit.data.local.dao.MechanicalUnitDao
import net.calvuz.qreport.client.unit.data.local.mapper.MechanicalUnitMapper
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MechanicalUnitRepositoryImpl @Inject constructor(
    private val dao: MechanicalUnitDao,
    private val mapper: MechanicalUnitMapper
) : MechanicalUnitRepository {

    override fun getForIslandFlow(islandId: String): Flow<List<MechanicalUnit>> =
        dao.getForIslandFlow(islandId).map { mapper.toDomainList(it) }

    override suspend fun getMechanicalUnitById(id: String): Result<MechanicalUnit?> = runCatching {
        dao.getById(id)?.let { mapper.toDomain(it) }
    }

    override suspend fun create(unit: MechanicalUnit): Result<Unit> = runCatching {
        dao.insert(mapper.toEntity(unit))
    }

    override suspend fun update(unit: MechanicalUnit): Result<Unit> = runCatching {
        dao.update(mapper.toEntity(unit.copy(updatedAt = Clock.System.now())))
    }

    override suspend fun delete(unit: MechanicalUnit): Result<Unit> = runCatching {
        dao.delete(mapper.toEntity(unit.copy(updatedAt = Clock.System.now())))
    }

    // ===== DELETE — TWO-STAGE =====

    /** Stage 1: sets isActive=false. No children to cascade. */
    override suspend fun deactivateUnit(id: String): Result<Unit> = runCatching {
        dao.deactivateUnit(id, Clock.System.now().toEpochMilliseconds())
    }

    /** Stage 2: sets isDeleted=true for server sync. */
    override suspend fun markUnitDeleted(id: String): Result<Unit> =  runCatching  {
        dao.markUnitDeleted(id, Clock.System.now().toEpochMilliseconds())
    }

    // ===== SEARCH =====

    override suspend fun getUnitsByIsland(islandId: String): Result<List<MechanicalUnit>> {
            return try {
                val entities = dao.getMechanicalUnitsForIsland(islandId)
                val units = mapper.toDomainList(entities)
                Result.success(units)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ===== FLOW =====

    override fun getAllActiveMechanicalUnitFlow(): Flow<List<MechanicalUnit>> {
        return dao.getAllActiveMechanicalUnitFlow().map { entities ->
            mapper.toDomainList(entities)
        }
    }

    override fun getAllActiveMechanicalUnitByIslandFlow(islandId: String): Flow<List<MechanicalUnit>> {
        return dao.getAllActiveMechanicalUnitByIslandFlow(islandId).map { entities ->
            mapper.toDomainList(entities)
        }
    }
}