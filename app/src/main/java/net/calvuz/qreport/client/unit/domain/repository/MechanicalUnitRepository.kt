package net.calvuz.qreport.client.unit.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit

/**
 * Repository interface for [MechanicalUnit].
 */
interface MechanicalUnitRepository {

    /** Live list of active units for the given island. */
    fun getForIslandFlow(islandId: String): Flow<List<MechanicalUnit>>

    suspend fun getMechanicalUnitById(id: String): Result<MechanicalUnit?>

    suspend fun create(unit: MechanicalUnit): Result<Unit>

    suspend fun update(unit: MechanicalUnit): Result<Unit>

    suspend fun delete(unit: MechanicalUnit): Result<Unit>

    // ===== DELETE — TWO-STAGE =====

    /** Stage 1: sets isActive=false. No children to cascade. */
    suspend fun deactivateUnit(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>
    /** Stage 2: sets isDeleted=true for server sync. */
    suspend fun markUnitDeleted(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    // ===== RESTORE =====
    
    suspend fun restoreUnit(id: String, ts: Long = System.currentTimeMillis()): Result<Unit>

    // ===== SEARCH =====

    suspend fun getUnitById(id: String): Result<MechanicalUnit?>
    suspend fun getUnitsByIsland(islandId: String): Result<List<MechanicalUnit>>

    // ===== Flow =====

    fun getAMechanicalUnitFlow(): Flow<List<MechanicalUnit>>
    fun geteMechanicalUnitByIslandFlow(islandId: String): Flow<List<MechanicalUnit>>
    fun getAllActiveMechanicalUnitByIslandFlow(islandId: String): Flow<List<MechanicalUnit>>
    fun getAllActiveMechanicalUnitFlow(): Flow<List<MechanicalUnit>>
}

