package net.calvuz.qreport.checkup.status.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster

/**
 * Repository for the checkup_statuses master data list (create/edit/deactivate
 * from Settings) plus the checkup_status_transitions workflow graph.
 */
interface CheckUpStatusMasterRepository {

    fun observeCheckUpStatuses(): Flow<List<CheckUpStatusMaster>>

    /** Active statuses only — for chip/filter rendering and dropdowns. */
    fun observeActiveCheckUpStatuses(): Flow<List<CheckUpStatusMaster>>

    suspend fun getById(id: String): Result<CheckUpStatusMaster?>

    suspend fun getByCode(code: String): Result<CheckUpStatusMaster?>

    suspend fun createCheckUpStatus(status: CheckUpStatusMaster): Result<Unit>

    suspend fun updateCheckUpStatus(status: CheckUpStatusMaster): Result<Unit>

    suspend fun deactivateCheckUpStatus(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    suspend fun restoreCheckUpStatus(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    /** All transitions, grouped by origin status id — for the transitions management screen. */
    fun observeTransitions(): Flow<Map<String, List<String>>>

    suspend fun isTransitionAllowed(fromId: String, toId: String): Boolean

    /** Replaces the set of statuses reachable from [fromId]. */
    suspend fun setAllowedTransitions(fromId: String, toIds: List<String>): Result<Unit>
}
