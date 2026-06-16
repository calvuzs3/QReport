package net.calvuz.qreport.checkup.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.domain.model.CheckUpMaintenanceLogAssociation

interface CheckUpMaintenanceLogAssociationRepository {

    // ===== CREATE =====

    suspend fun createAssociation(checkupId: String, maintenanceLogId: String): Result<String>

    // ===== DELETE =====

    suspend fun deleteAssociation(associationId: String): Result<Unit>
    suspend fun deleteAssociationsByCheckUp(checkupId: String): Result<Unit>
    suspend fun deleteAssociationsByLog(maintenanceLogId: String): Result<Unit>
    suspend fun deleteSpecificAssociation(checkupId: String, maintenanceLogId: String): Result<Unit>

    // ===== READ =====

    suspend fun getAssociation(checkupId: String, maintenanceLogId: String): CheckUpMaintenanceLogAssociation?

    suspend fun getAssociationsByCheckUp(checkupId: String): List<CheckUpMaintenanceLogAssociation>
    fun getAssociationsByCheckUpFlow(checkupId: String): Flow<List<CheckUpMaintenanceLogAssociation>>

    suspend fun getAssociationsByLog(maintenanceLogId: String): List<CheckUpMaintenanceLogAssociation>

    // ===== VALIDATION =====

    suspend fun isAssociated(checkupId: String, maintenanceLogId: String): Boolean
    suspend fun getLogCount(checkupId: String): Int

    // ===== ADVANCED =====

    suspend fun getLogIdsForCheckUp(checkupId: String): List<String>
    suspend fun getCheckUpIdsForLog(maintenanceLogId: String): List<String>
}
