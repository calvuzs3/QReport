package net.calvuz.qreport.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.domain.model.checkup.CheckUpIslandAssociation
import net.calvuz.qreport.domain.model.checkup.AssociationType

/**
 * Repository per gestire le associazioni CheckUp-Isole
 * Seguendo principi Clean Architecture
 */
interface CheckUpAssociationRepository {

    // ===== CREATE =====

    /**
     * Crea associazione singola CheckUp-Isola
     */
    suspend fun createAssociation(
        checkupId: String,
        islandId: String,
        associationType: AssociationType = AssociationType.STANDARD,
        notes: String? = null
    ): Result<String>

    /**
     * Crea associazioni multiple per CheckUp Multi-Isola
     */
    suspend fun createMultipleAssociations(
        checkupId: String,
        islandIds: List<String>,
        associationType: AssociationType = AssociationType.MULTI_ISLAND,
        notes: String? = null
    ): Result<List<String>>

    // ===== DELETE =====

    suspend fun deleteAssociation(associationId: String): Result<Unit>
    suspend fun deleteAssociationsByCheckUp(checkupId: String): Result<Unit>
    suspend fun deleteAssociationsByIsland(islandId: String): Result<Unit>
    suspend fun deleteSpecificAssociation(checkupId: String, islandId: String): Result<Unit>

    // ===== READ =====

    suspend fun getAssociation(associationId: String): CheckUpIslandAssociation?
    suspend fun getAssociation(checkupId: String, islandId: String): CheckUpIslandAssociation?

    suspend fun getAssociationsByCheckUp(checkupId: String): List<CheckUpIslandAssociation>
    fun getAssociationsByCheckUpFlow(checkupId: String): Flow<List<CheckUpIslandAssociation>>

    suspend fun getAssociationsByIsland(islandId: String): List<CheckUpIslandAssociation>
    fun getAssociationsByIslandFlow(islandId: String): Flow<List<CheckUpIslandAssociation>>

    // ===== VALIDATION =====

    suspend fun isAssociated(checkupId: String, islandId: String): Boolean
    suspend fun hasAssociations(checkupId: String): Boolean
    suspend fun getAssociationCount(checkupId: String): Int

    // ===== STATISTICS =====

    suspend fun getCheckUpCountForIsland(islandId: String): Int
    suspend fun getCheckUpCountForClient(clientId: String): Int

    // ===== ADVANCED QUERIES =====

    suspend fun getUnassociatedCheckUpIds(): List<String>
    fun getUnassociatedCheckUpIdsFlow(): Flow<List<String>>

    suspend fun getIslandIdsForCheckUp(checkupId: String): List<String>
    suspend fun getCheckUpIdsForIsland(islandId: String): List<String>

    suspend fun getRecentAssociationsForIsland(islandId: String, limit: Int = 10): List<CheckUpIslandAssociation>
    suspend fun getRecentAssociationsForClient(clientId: String, limit: Int = 20): List<CheckUpIslandAssociation>
}