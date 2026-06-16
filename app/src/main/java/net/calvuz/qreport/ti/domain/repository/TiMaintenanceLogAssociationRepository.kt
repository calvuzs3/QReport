package net.calvuz.qreport.ti.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.ti.domain.model.TiMaintenanceLogAssociation

interface TiMaintenanceLogAssociationRepository {

    // ===== CREATE =====

    suspend fun createAssociation(interventionId: String, maintenanceLogId: String): Result<String>

    // ===== DELETE =====

    suspend fun deleteAssociation(associationId: String): Result<Unit>
    suspend fun deleteAssociationsByIntervention(interventionId: String): Result<Unit>
    suspend fun deleteAssociationsByLog(maintenanceLogId: String): Result<Unit>
    suspend fun deleteSpecificAssociation(interventionId: String, maintenanceLogId: String): Result<Unit>

    // ===== READ =====

    suspend fun getAssociation(interventionId: String, maintenanceLogId: String): TiMaintenanceLogAssociation?

    suspend fun getAssociationsByIntervention(interventionId: String): List<TiMaintenanceLogAssociation>
    fun getAssociationsByInterventionFlow(interventionId: String): Flow<List<TiMaintenanceLogAssociation>>

    suspend fun getAssociationsByLog(maintenanceLogId: String): List<TiMaintenanceLogAssociation>

    // ===== VALIDATION =====

    suspend fun isAssociated(interventionId: String, maintenanceLogId: String): Boolean
    suspend fun getLogCount(interventionId: String): Int

    // ===== ADVANCED =====

    suspend fun getLogIdsForIntervention(interventionId: String): List<String>
    suspend fun getInterventionIdsForLog(maintenanceLogId: String): List<String>
}
