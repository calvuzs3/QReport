package net.calvuz.qreport.client.unit.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import net.calvuz.qreport.client.island.domain.model.Island
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

    override suspend fun getById(id: String): MechanicalUnit? =
        dao.getById(id)?.let { mapper.toDomain(it) }

    override suspend fun create(unit: MechanicalUnit): Result<Unit> = runCatching {
        dao.insert(mapper.toEntity(unit))
    }

    override suspend fun update(unit: MechanicalUnit): Result<Unit> = runCatching {
        dao.update(mapper.toEntity(unit.copy(updatedAt = Clock.System.now())))
    }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        dao.softDelete(id, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun getUnitsByIsland(islandId: String): Result<List<MechanicalUnit>> {
            return try {
                val entities = dao.getMechanicalUnitsForIsland(islandId)
                val units = mapper.toDomainList(entities)
                Result.success(units)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

