package net.calvuz.qreport.checkup.status.data.local.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.calvuz.qreport.checkup.status.data.local.dao.CheckUpStatusDao
import net.calvuz.qreport.checkup.status.data.local.mapper.CheckUpStatusMapper
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster
import net.calvuz.qreport.checkup.status.domain.repository.CheckUpStatusMasterRepository
import javax.inject.Inject

class CheckUpStatusMasterRepositoryImpl @Inject constructor(
    private val checkUpStatusDao: CheckUpStatusDao,
    private val mapper: CheckUpStatusMapper
) : CheckUpStatusMasterRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allCheckUpStatuses: Flow<List<CheckUpStatusMaster>> =
        checkUpStatusDao.observeAllCheckUpStatuses()
            .map { mapper.toDomainList(it) }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    private val activeCheckUpStatuses: Flow<List<CheckUpStatusMaster>> =
        checkUpStatusDao.observeActiveCheckUpStatuses()
            .map { mapper.toDomainList(it) }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    private val allTransitions: Flow<Map<String, List<String>>> =
        checkUpStatusDao.observeAllTransitions()
            .map { rows -> rows.groupBy({ it.fromStatusId }, { it.toStatusId }) }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyMap())

    override fun observeCheckUpStatuses(): Flow<List<CheckUpStatusMaster>> = allCheckUpStatuses

    override fun observeActiveCheckUpStatuses(): Flow<List<CheckUpStatusMaster>> = activeCheckUpStatuses

    override suspend fun getById(id: String): Result<CheckUpStatusMaster?> = try {
        Result.success(checkUpStatusDao.getById(id)?.let { mapper.toDomain(it) })
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getByCode(code: String): Result<CheckUpStatusMaster?> = try {
        Result.success(checkUpStatusDao.getByCode(code)?.let { mapper.toDomain(it) })
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createCheckUpStatus(status: CheckUpStatusMaster): Result<Unit> = try {
        checkUpStatusDao.insert(mapper.toEntity(status))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateCheckUpStatus(status: CheckUpStatusMaster): Result<Unit> = try {
        checkUpStatusDao.update(mapper.toEntity(status))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deactivateCheckUpStatus(id: String, ts: Long): Result<Unit> = try {
        checkUpStatusDao.deactivate(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun restoreCheckUpStatus(id: String, ts: Long): Result<Unit> = try {
        checkUpStatusDao.restore(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeTransitions(): Flow<Map<String, List<String>>> = allTransitions

    override suspend fun isTransitionAllowed(fromId: String, toId: String): Boolean =
        checkUpStatusDao.isTransitionAllowed(fromId, toId)

    override suspend fun setAllowedTransitions(fromId: String, toIds: List<String>): Result<Unit> = try {
        checkUpStatusDao.replaceTransitionsFrom(fromId, toIds)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
