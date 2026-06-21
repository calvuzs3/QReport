package net.calvuz.qreport.checkup.criticality.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster

/**
 * Repository for the criticality_levels master data list (create/edit/deactivate
 * from Settings, and lookups used wherever a criticality level needs resolving).
 */
interface CriticalityMasterRepository {

    fun observeCriticalityLevels(): Flow<List<CriticalityMaster>>

    /** Active levels only — for selection dropdowns (e.g. template editor). */
    fun observeActiveCriticalityLevels(): Flow<List<CriticalityMaster>>

    suspend fun getCriticalityLevels(): Result<List<CriticalityMaster>>

    suspend fun getByCode(code: String): Result<CriticalityMaster?>

    suspend fun createCriticalityLevel(level: CriticalityMaster): Result<Unit>

    suspend fun updateCriticalityLevel(level: CriticalityMaster): Result<Unit>

    suspend fun deactivateCriticalityLevel(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    suspend fun restoreCriticalityLevel(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>
}
