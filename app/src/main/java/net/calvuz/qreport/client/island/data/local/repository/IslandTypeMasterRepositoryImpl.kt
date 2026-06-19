package net.calvuz.qreport.client.island.data.local.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.calvuz.qreport.client.island.data.local.dao.IslandTypeDao
import net.calvuz.qreport.client.island.data.local.mapper.IslandTypeMapper
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import javax.inject.Inject

class IslandTypeMasterRepositoryImpl @Inject constructor(
    private val islandTypeDao: IslandTypeDao,
    private val mapper: IslandTypeMapper
) : IslandTypeMasterRepository {

    /**
     * Scoped to this singleton's own lifetime (the whole app process) — backs the
     * two shared StateFlows below so every screen attaches to the same running Room
     * query instead of each ViewModel triggering its own subscription.
     */
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allIslandTypes: Flow<List<IslandTypeMaster>> =
        islandTypeDao.observeAllIslandTypes()
            .map { mapper.toDomainList(it) }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    private val activeIslandTypes: Flow<List<IslandTypeMaster>> =
        islandTypeDao.observeActiveIslandTypes()
            .map { mapper.toDomainList(it) }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    override fun observeIslandTypes(): Flow<List<IslandTypeMaster>> = allIslandTypes

    override fun observeActiveIslandTypes(): Flow<List<IslandTypeMaster>> = activeIslandTypes

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
