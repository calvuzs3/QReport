package net.calvuz.qreport.ti.data.local.repository

import kotlinx.datetime.Clock
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for TechnicalIntervention using Room database
 * Follows Result<T> pattern for error handling consistency with QReport
 */
@Singleton
class TechnicalInterventionRepositoryImpl @Inject constructor(
    private val interventionDao: TechnicalInterventionDao
) : TechnicalInterventionRepository {

    override suspend fun createIntervention(intervention: TechnicalIntervention): Result<String> {
        return try {
            val entity = intervention.toEntity()
            interventionDao.insertIntervention(entity)
            Result.success(intervention.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInterventionById(id: String): Result<TechnicalIntervention> {
        return try {
            val entity = interventionDao.getInterventionById(id)
            if (entity != null) {
                Result.success(entity.toDomain())
            } else {
                Result.failure(NoSuchElementException("Intervention with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastInterventionNumber(): Result<String?> {
        return try {
            val lastNumber = interventionDao.getLastInterventionNumber()
            Result.success(lastNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllInterventions(): Result<List<TechnicalIntervention>> {
        return try {
            val entities = interventionDao.getAllInterventions()
            val interventions = entities.map { it.toDomain() }
            Result.success(interventions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInterventionsByStatus(status: InterventionStatus): Result<List<TechnicalIntervention>> {
        return try {
            val entities = interventionDao.getInterventionsByStatus(status)
            val interventions = entities.map { it.toDomain() }
            Result.success(interventions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateIntervention(intervention: TechnicalIntervention): Result<Unit> {
        return try {
            val updatedIntervention = intervention.copy(
                updatedAt = Clock.System.now()
            )
            val entity = updatedIntervention.toEntity()
            interventionDao.updateIntervention(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteIntervention(id: String): Result<Unit> {
        return try {
            val entity = interventionDao.getInterventionById(id)
            if (entity != null) {
                interventionDao.deleteIntervention(entity)
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("Intervention with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== ADDITIONAL HELPER METHODS =====

    /**
     * Get active interventions (DRAFT, IN_PROGRESS)
     */
    suspend fun getActiveInterventions(): Result<List<TechnicalIntervention>> {
        return try {
            val entities = interventionDao.getActiveInterventions()
            val interventions = entities.map { it.toDomain() }
            Result.success(interventions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get completed interventions
     */
    suspend fun getCompletedInterventions(): Result<List<TechnicalIntervention>> {
        return try {
            val entities = interventionDao.getCompletedInterventions()
            val interventions = entities.map { it.toDomain() }
            Result.success(interventions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search interventions by customer name or intervention number
     */
    suspend fun searchInterventions(query: String): Result<List<TechnicalIntervention>> {
        return try {
            val entities = interventionDao.searchInterventions(query, query)
            val interventions = entities.map { it.toDomain() }
            Result.success(interventions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of interventions by status
     */
    suspend fun getCountByStatus(status: InterventionStatus): Result<Int> {
        return try {
            val count = interventionDao.countByStatus(status)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of completed interventions
     */
    suspend fun getCompletedCount(): Result<Int> {
        return try {
            val count = interventionDao.countCompleted()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}