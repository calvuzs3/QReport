package net.calvuz.qreport.checkup.modules.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster

/**
 * Repository for the module_types master data list (create/edit/deactivate from
 * Settings, and lookups used wherever a checklist module type needs resolving).
 */
interface ModuleTypeMasterRepository {

    fun observeModuleTypes(): Flow<List<ModuleTypeMaster>>

    /** Active types only — for selection dropdowns (e.g. template editor). */
    fun observeActiveModuleTypes(): Flow<List<ModuleTypeMaster>>

    suspend fun getModuleTypes(): Result<List<ModuleTypeMaster>>

    suspend fun getByCode(code: String): Result<ModuleTypeMaster?>

    suspend fun createModuleType(type: ModuleTypeMaster): Result<Unit>

    suspend fun updateModuleType(type: ModuleTypeMaster): Result<Unit>

    suspend fun deactivateModuleType(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    suspend fun restoreModuleType(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    /** All module↔island-type links, grouped by island type id — for the association management screen. */
    fun observeModuleIslandTypeLinks(): Flow<Map<String, List<String>>>

    /** Modules associated with a given island type — used to seed a new checkup's checklist. */
    suspend fun getModuleTypeIdsForIslandType(islandTypeId: String): Result<List<String>>

    /** Replaces all module links for a given island type. */
    suspend fun setModuleTypesForIslandType(islandTypeId: String, moduleTypeIds: List<String>): Result<Unit>
}
