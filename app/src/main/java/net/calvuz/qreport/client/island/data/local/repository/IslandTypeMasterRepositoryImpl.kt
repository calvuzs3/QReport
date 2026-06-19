package net.calvuz.qreport.client.island.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.island.data.local.dao.IslandTypeDao
import net.calvuz.qreport.client.island.data.local.mapper.IslandTypeMapper
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import javax.inject.Inject

class IslandTypeMasterRepositoryImpl @Inject constructor(
    private val islandTypeDao: IslandTypeDao,
    private val mapper: IslandTypeMapper
) : IslandTypeMasterRepository {

    override fun observeIslandTypes(): Flow<List<IslandTypeMaster>> =
        islandTypeDao.observeAllIslandTypes().map { mapper.toDomainList(it) }

    override fun observeActiveIslandTypes(): Flow<List<IslandTypeMaster>> =
        islandTypeDao.observeActiveIslandTypes().map { mapper.toDomainList(it) }

    override suspend fun getIslandTypes(): Result<List<IslandTypeMaster>> = try {
        Result.success(mapper.toDomainList(islandTypeDao.getAllIslandTypes()))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getByCode(code: String): Result<IslandTypeMaster?> = try {
        Result.success(islandTypeDao.getByCode(code)?.let { mapper.toDomain(it) })
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createIslandType(type: IslandTypeMaster): Result<Unit> = try {
        islandTypeDao.insert(mapper.toEntity(type))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateIslandType(type: IslandTypeMaster): Result<Unit> = try {
        islandTypeDao.update(mapper.toEntity(type))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deactivateIslandType(id: String, ts: Long): Result<Unit> = try {
        islandTypeDao.deactivate(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun restoreIslandType(id: String, ts: Long): Result<Unit> = try {
        islandTypeDao.restore(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
