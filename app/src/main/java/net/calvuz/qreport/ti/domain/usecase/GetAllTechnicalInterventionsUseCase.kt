package net.calvuz.qreport.ti.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
import javax.inject.Inject

/**
 * Use Case: Get All Technical Interventions with filtering support
 *
 * Returns all interventions or filtered by status.
 * Default filter: DRAFT + IN_PROGRESS (active interventions)
 */
class GetAllTechnicalInterventionsUseCase @Inject constructor(
    private val interventionRepository: TechnicalInterventionRepository
) {

    /**
     * Get all technical interventions
     *
     * @return Flow with QrResult containing list of TechnicalIntervention or error
     */
    operator fun invoke(): Flow<QrResult<List<TechnicalIntervention>, QrError>> = flow {
        try {
            emit(QrResult.Loading())

            val result = interventionRepository.getAllInterventions()

            if (result.isSuccess) {
                val interventions = result.getOrThrow()
                emit(QrResult.Success(interventions))
            } else {
                val exception = result.exceptionOrNull()
                emit(QrResult.Error(QrError.InterventionError.LOAD(exception?.message)))
            }

        } catch (e: Exception) {
            emit(QrResult.Error(QrError.InterventionError.LOAD(e.message)))
        }
    }

    /**
     * Get active interventions (DRAFT + IN_PROGRESS) - Default filter
     *
     * @return Flow with QrResult containing active interventions
     */
    fun getActiveInterventions(): Flow<QrResult<List<TechnicalIntervention>, QrError>> = flow {
        try {
            emit(QrResult.Loading())

            // Get DRAFT interventions
            val draftResult = interventionRepository.getInterventionsByStatus(InterventionStatus.DRAFT)
            val inProgressResult = interventionRepository.getInterventionsByStatus(InterventionStatus.IN_PROGRESS)

            if (draftResult.isSuccess && inProgressResult.isSuccess) {
                val draftInterventions = draftResult.getOrThrow()
                val inProgressInterventions = inProgressResult.getOrThrow()

                val activeInterventions = (draftInterventions + inProgressInterventions)
                    .sortedByDescending { it.updatedAt }

                emit(QrResult.Success(activeInterventions))
            } else {
                val error = draftResult.exceptionOrNull() ?: inProgressResult.exceptionOrNull()
                emit(QrResult.Error(QrError.InterventionError.LOAD(error?.message)))
            }

        } catch (e: Exception) {
            emit(QrResult.Error(QrError.InterventionError.LOAD(e.message)))
        }
    }

    /**
     * Get interventions by specific status
     *
     * @param status InterventionStatus to filter by
     * @return Flow with QrResult containing filtered interventions
     */
    fun getInterventionsByStatus(status: InterventionStatus): Flow<QrResult<List<TechnicalIntervention>, QrError>> = flow {
        try {
            emit(QrResult.Loading())

            val result = interventionRepository.getInterventionsByStatus(status)

            if (result.isSuccess) {
                val interventions = result.getOrThrow().sortedByDescending { it.updatedAt }
                emit(QrResult.Success(interventions))
            } else {
                val exception = result.exceptionOrNull()
                emit(QrResult.Error(QrError.InterventionError.LOAD(exception?.message)))
            }

        } catch (e: Exception) {
            emit(QrResult.Error(QrError.InterventionError.LOAD(e.message)))
        }
    }

    /**
     * Get completed interventions
     *
     * @return Flow with QrResult containing completed interventions
     */
    fun getCompletedInterventions(): Flow<QrResult<List<TechnicalIntervention>, QrError>> = flow {
        try {
            emit(QrResult.Loading())

            // Get COMPLETED and ARCHIVED interventions
            val completedResult = interventionRepository.getInterventionsByStatus(InterventionStatus.COMPLETED)
            val archivedResult = interventionRepository.getInterventionsByStatus(InterventionStatus.ARCHIVED)

            if (completedResult.isSuccess && archivedResult.isSuccess) {
                val completedInterventions = completedResult.getOrThrow()
                val archivedInterventions = archivedResult.getOrThrow()

                val allCompleted = (completedInterventions + archivedInterventions)
                    .sortedByDescending { it.updatedAt }

                emit(QrResult.Success(allCompleted))
            } else {
                val error = completedResult.exceptionOrNull() ?: archivedResult.exceptionOrNull()
                emit(QrResult.Error(QrError.InterventionError.LOAD(error?.message)))
            }

        } catch (e: Exception) {
            emit(QrResult.Error(QrError.InterventionError.LOAD(e.message)))
        }
    }
}