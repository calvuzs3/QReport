package net.calvuz.qreport.client.island.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.island.data.local.entity.IslandTypeEntity

/**
 * Repository for the island_types master data list (create/edit/deactivate from Settings).
 *
 * Operates directly on [IslandTypeEntity] — these are flat reference records with no
 * business logic, so a separate domain model would just be a 1:1 copy.
 */
interface IslandTypeMasterRepository {

    fun observeIslandTypes(): Flow<List<IslandTypeEntity>>

    /** Active types only — for selection dropdowns (e.g. island creation form). */
    fun observeActiveIslandTypes(): Flow<List<IslandTypeEntity>>

    suspend fun getIslandTypes(): Result<List<IslandTypeEntity>>

    suspend fun getByCode(code: String): Result<IslandTypeEntity?>

    suspend fun createIslandType(type: IslandTypeEntity): Result<Unit>

    suspend fun updateIslandType(type: IslandTypeEntity): Result<Unit>

    suspend fun deactivateIslandType(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    suspend fun restoreIslandType(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>
}
