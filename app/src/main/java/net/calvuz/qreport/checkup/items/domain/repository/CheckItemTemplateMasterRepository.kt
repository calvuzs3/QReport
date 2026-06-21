package net.calvuz.qreport.checkup.items.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.items.domain.model.CheckItemTemplateMaster

/**
 * Repository for the check_item_templates master data list (create/edit/deactivate
 * from Settings, and lookups used when generating a checkup's checklist).
 */
interface CheckItemTemplateMasterRepository {

    fun observeTemplates(): Flow<List<CheckItemTemplateMaster>>

    /** Active templates only — for the template editor's reference list. */
    fun observeActiveTemplates(): Flow<List<CheckItemTemplateMaster>>

    suspend fun getTemplates(): Result<List<CheckItemTemplateMaster>>

    /** Active templates belonging to one of the given modules — used to seed a new checkup's checklist. */
    suspend fun getTemplatesForModuleTypes(moduleTypeIds: List<String>): Result<List<CheckItemTemplateMaster>>

    suspend fun createTemplate(template: CheckItemTemplateMaster): Result<Unit>

    suspend fun updateTemplate(template: CheckItemTemplateMaster): Result<Unit>

    suspend fun deactivateTemplate(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    suspend fun restoreTemplate(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>
}
