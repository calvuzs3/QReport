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

    /** Soft-delete: sets isActive = false. */
    suspend fun delete(id: String): Result<Unit>

    suspend fun getUnitsByIsland(islandId: String): Result<List<MechanicalUnit>>
    fun getAllActiveMechanicalUnitByIslandFlow(islandId: String): Flow<List<MechanicalUnit>>
    fun getAllActiveMechanicalUnitFlow(): Flow<List<MechanicalUnit>>
}

