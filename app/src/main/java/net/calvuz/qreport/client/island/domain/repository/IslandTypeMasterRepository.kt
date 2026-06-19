package net.calvuz.qreport.client.island.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster

/**
 * Repository for the island_types master data list (create/edit/deactivate from Settings,
 * and lookups used everywhere an island type needs to be displayed or resolved).
 */
interface IslandTypeMasterRepository {

    fun observeIslandTypes(): Flow<List<IslandTypeMaster>>

    /** Active types only — for selection dropdowns (e.g. island creation form). */
    fun observeActiveIslandTypes(): Flow<List<IslandTypeMaster>>

    suspend fun getIslandTypes(): Result<List<IslandTypeMaster>>

    suspend fun getByCode(code: String): Result<IslandTypeMaster?>

    suspend fun createIslandType(type: IslandTypeMaster): Result<Unit>

    suspend fun updateIslandType(type: IslandTypeMaster): Result<Unit>

    suspend fun deactivateIslandType(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    suspend fun restoreIslandType(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>
}
