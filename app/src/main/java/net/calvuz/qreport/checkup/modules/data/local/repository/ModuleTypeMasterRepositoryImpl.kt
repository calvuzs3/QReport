package net.calvuz.qreport.checkup.modules.data.local.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.calvuz.qreport.checkup.modules.data.local.dao.ModuleTypeDao
import net.calvuz.qreport.checkup.modules.data.local.mapper.ModuleTypeMapper
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.checkup.modules.domain.repository.ModuleTypeMasterRepository
import javax.inject.Inject

class ModuleTypeMasterRepositoryImpl @Inject constructor(
    private val moduleTypeDao: ModuleTypeDao,
    private val mapper: ModuleTypeMapper
) : ModuleTypeMasterRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allModuleTypes: Flow<List<ModuleTypeMaster>> =
        moduleTypeDao.observeAllModuleTypes()
            .map { mapper.toDomainList(it) }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    private val activeModuleTypes: Flow<List<ModuleTypeMaster>> =
        moduleTypeDao.observeActiveModuleTypes()
            .map { mapper.toDomainList(it) }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    override fun observeModuleTypes(): Flow<List<ModuleTypeMaster>> = allModuleTypes

    override fun observeActiveModuleTypes(): Flow<List<ModuleTypeMaster>> = activeModuleTypes

    override suspend fun getModuleTypes(): Result<List<ModuleTypeMaster>> = try {
        Result.success(mapper.toDomainList(moduleTypeDao.getAllModuleTypes()))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getByCode(code: String): Result<ModuleTypeMaster?> = try {
        Result.success(moduleTypeDao.getByCode(code)?.let { mapper.toDomain(it) })
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createModuleType(type: ModuleTypeMaster): Result<Unit> = try {
        moduleTypeDao.insert(mapper.toEntity(type))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateModuleType(type: ModuleTypeMaster): Result<Unit> = try {
        moduleTypeDao.update(mapper.toEntity(type))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deactivateModuleType(id: String, ts: Long): Result<Unit> = try {
        moduleTypeDao.deactivate(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun restoreModuleType(id: String, ts: Long): Result<Unit> = try {
        moduleTypeDao.restore(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeModuleIslandTypeLinks(): Flow<Map<String, List<String>>> =
        moduleTypeDao.observeAllModuleIslandLinks()
            .map { links -> links.groupBy({ it.islandTypeId }, { it.moduleTypeId }) }

    override suspend fun getModuleTypeIdsForIslandType(islandTypeId: String): Result<List<String>> = try {
        Result.success(moduleTypeDao.getModuleTypeIdsForIslandType(islandTypeId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun setModuleTypesForIslandType(islandTypeId: String, moduleTypeIds: List<String>): Result<Unit> = try {
        moduleTypeDao.replaceModuleIslandLinks(islandTypeId, moduleTypeIds)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
