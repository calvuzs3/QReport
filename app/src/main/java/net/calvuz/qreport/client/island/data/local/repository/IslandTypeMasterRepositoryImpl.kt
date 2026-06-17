package net.calvuz.qreport.client.island.data.local.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.island.data.local.dao.IslandTypeDao
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import javax.inject.Inject

class IslandTypeMasterRepositoryImpl @Inject constructor(
    private val islandTypeDao: IslandTypeDao
) : IslandTypeMasterRepository {

    override fun observeIslandTypes(): Flow<List<IslandTypeEntity>> =
        islandTypeDao.observeAllIslandTypes()

    override fun observeActiveIslandTypes(): Flow<List<IslandTypeEntity>> =
        islandTypeDao.observeActiveIslandTypes()

    override suspend fun getIslandTypes(): Result<List<IslandTypeEntity>> = try {
        Result.success(islandTypeDao.getAllIslandTypes())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getByCode(code: String): Result<IslandTypeEntity?> = try {
        Result.success(islandTypeDao.getByCode(code))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createIslandType(type: IslandTypeEntity): Result<Unit> = try {
        islandTypeDao.insert(type)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateIslandType(type: IslandTypeEntity): Result<Unit> = try {
        islandTypeDao.update(type)
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
