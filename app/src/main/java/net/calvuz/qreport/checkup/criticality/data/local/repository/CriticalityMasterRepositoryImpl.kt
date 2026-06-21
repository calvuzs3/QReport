package net.calvuz.qreport.checkup.criticality.data.local.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.calvuz.qreport.checkup.criticality.data.local.dao.CriticalityDao
import net.calvuz.qreport.checkup.criticality.data.local.mapper.CriticalityMapper
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster
import net.calvuz.qreport.checkup.criticality.domain.repository.CriticalityMasterRepository
import javax.inject.Inject

class CriticalityMasterRepositoryImpl @Inject constructor(
    private val criticalityDao: CriticalityDao,
    private val mapper: CriticalityMapper
) : CriticalityMasterRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allCriticalityLevels: Flow<List<CriticalityMaster>> =
        criticalityDao.observeAllCriticalityLevels()
            .map { mapper.toDomainList(it) }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    private val activeCriticalityLevels: Flow<List<CriticalityMaster>> =
        criticalityDao.observeActiveCriticalityLevels()
            .map { mapper.toDomainList(it) }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    override fun observeCriticalityLevels(): Flow<List<CriticalityMaster>> = allCriticalityLevels

    override fun observeActiveCriticalityLevels(): Flow<List<CriticalityMaster>> = activeCriticalityLevels

    override suspend fun getCriticalityLevels(): Result<List<CriticalityMaster>> = try {
        Result.success(mapper.toDomainList(criticalityDao.getAllCriticalityLevels()))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getByCode(code: String): Result<CriticalityMaster?> = try {
        Result.success(criticalityDao.getByCode(code)?.let { mapper.toDomain(it) })
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createCriticalityLevel(level: CriticalityMaster): Result<Unit> = try {
        criticalityDao.insert(mapper.toEntity(level))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateCriticalityLevel(level: CriticalityMaster): Result<Unit> = try {
        criticalityDao.update(mapper.toEntity(level))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deactivateCriticalityLevel(id: String, ts: Long): Result<Unit> = try {
        criticalityDao.deactivate(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun restoreCriticalityLevel(id: String, ts: Long): Result<Unit> = try {
        criticalityDao.restore(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
