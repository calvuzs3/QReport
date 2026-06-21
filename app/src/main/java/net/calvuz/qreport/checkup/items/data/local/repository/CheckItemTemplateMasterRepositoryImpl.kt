package net.calvuz.qreport.checkup.items.data.local.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.calvuz.qreport.checkup.items.data.local.dao.CheckItemTemplateDao
import net.calvuz.qreport.checkup.items.data.local.mapper.CheckItemTemplateMapper
import net.calvuz.qreport.checkup.items.domain.model.CheckItemTemplateMaster
import net.calvuz.qreport.checkup.items.domain.repository.CheckItemTemplateMasterRepository
import javax.inject.Inject

class CheckItemTemplateMasterRepositoryImpl @Inject constructor(
    private val templateDao: CheckItemTemplateDao,
    private val mapper: CheckItemTemplateMapper
) : CheckItemTemplateMasterRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allTemplates: Flow<List<CheckItemTemplateMaster>> =
        templateDao.observeAllTemplates()
            .map { entities -> entities.map { mapper.toDomain(it) } }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    private val activeTemplates: Flow<List<CheckItemTemplateMaster>> =
        templateDao.observeActiveTemplates()
            .map { entities -> entities.map { mapper.toDomain(it) } }
            .stateIn(repositoryScope, SharingStarted.Lazily, emptyList())

    override fun observeTemplates(): Flow<List<CheckItemTemplateMaster>> = allTemplates

    override fun observeActiveTemplates(): Flow<List<CheckItemTemplateMaster>> = activeTemplates

    override suspend fun getTemplates(): Result<List<CheckItemTemplateMaster>> = try {
        Result.success(templateDao.getAllTemplates().map { mapper.toDomain(it) })
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTemplatesForModuleTypes(moduleTypeIds: List<String>): Result<List<CheckItemTemplateMaster>> = try {
        if (moduleTypeIds.isEmpty()) {
            Result.success(emptyList())
        } else {
            Result.success(templateDao.getTemplatesForModuleTypes(moduleTypeIds).map { mapper.toDomain(it) })
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createTemplate(template: CheckItemTemplateMaster): Result<Unit> = try {
        templateDao.insert(mapper.toEntity(template))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTemplate(template: CheckItemTemplateMaster): Result<Unit> = try {
        templateDao.update(mapper.toEntity(template))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deactivateTemplate(id: String, ts: Long): Result<Unit> = try {
        templateDao.deactivate(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun restoreTemplate(id: String, ts: Long): Result<Unit> = try {
        templateDao.restore(id, ts)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
