package net.calvuz.qreport.ti.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.ti.domain.model.TiAssociationType
import net.calvuz.qreport.ti.domain.model.TiIslandAssociation

interface TiAssociationRepository {

    // ===== CREATE =====

    suspend fun createAssociation(
        interventionId: String,
        islandId: String,
        associationType: TiAssociationType = TiAssociationType.STANDARD,
        notes: String? = null
    ): Result<String>

    // ===== DELETE =====

    suspend fun deleteAssociation(associationId: String): Result<Unit>
    suspend fun deleteAssociationsByIntervention(interventionId: String): Result<Unit>
    suspend fun deleteAssociationsByIsland(islandId: String): Result<Unit>
    suspend fun deleteSpecificAssociation(interventionId: String, islandId: String): Result<Unit>

    // ===== READ =====

    suspend fun getAssociation(associationId: String): TiIslandAssociation?
    suspend fun getAssociation(interventionId: String, islandId: String): TiIslandAssociation?

    suspend fun getAssociationsByIntervention(interventionId: String): List<TiIslandAssociation>
    fun getAssociationsByInterventionFlow(interventionId: String): Flow<List<TiIslandAssociation>>

    suspend fun getAssociationsByIsland(islandId: String): List<TiIslandAssociation>
    fun getAssociationsByIslandFlow(islandId: String): Flow<List<TiIslandAssociation>>

    // ===== VALIDATION =====

    suspend fun isAssociated(interventionId: String, islandId: String): Boolean
    suspend fun hasAssociations(interventionId: String): Boolean
    suspend fun getAssociationCount(interventionId: String): Int

    // ===== STATISTICS =====

    suspend fun getTiCountForIsland(islandId: String): Int
    suspend fun getTiCountForClient(clientId: String): Int

    // ===== ADVANCED =====

    suspend fun getIslandIdsForIntervention(interventionId: String): List<String>
    suspend fun getInterventionIdsForIsland(islandId: String): List<String>

    suspend fun getRecentAssociationsForIsland(islandId: String, limit: Int = 10): List<TiIslandAssociation>
    suspend fun getRecentAssociationsForClient(clientId: String, limit: Int = 20): List<TiIslandAssociation>
}
