package net.calvuz.qreport.ti.domain.repository

import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention

/**
 * Repository interface for TechnicalIntervention (using Result<T>)
 */
interface TechnicalInterventionRepository {
    suspend fun createIntervention(intervention: TechnicalIntervention): Result<String>
    suspend fun getInterventionById(id: String): Result<TechnicalIntervention>
    suspend fun getLastInterventionNumber(): Result<String?>
    suspend fun getAllInterventions(): Result<List<TechnicalIntervention>>
    suspend fun getInterventionsByStatus(status: InterventionStatus): Result<List<TechnicalIntervention>>
    suspend fun updateIntervention(intervention: TechnicalIntervention): Result<Unit>
    suspend fun deleteIntervention(id: String): Result<Unit>
}