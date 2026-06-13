package net.calvuz.qreport.ti.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
import javax.inject.Inject

/**
 * Use Case: Get All Technical Interventions with filtering support
 *
 * Returns all interventions or filtered by status, reactively (Room Flow),
 * so the list updates automatically whenever the underlying data changes.
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
    operator fun invoke(): Flow<QrResult<List<TechnicalIntervention>, QrError>> =
        interventionRepository.getAllInterventionsFlow()
            .map<List<TechnicalIntervention>, QrResult<List<TechnicalIntervention>, QrError>> { QrResult.Success(it) }
            .catch { e -> emit(QrResult.Error(QrError.InterventionError.LoadError(e.message))) }

    /**
     * Get active interventions (DRAFT + IN_PROGRESS) - Default filter
     *
     * @return Flow with QrResult containing active interventions
     */
    fun getActiveInterventions(): Flow<QrResult<List<TechnicalIntervention>, QrError>> =
        interventionRepository.getActiveInterventionsFlow()
            .map<List<TechnicalIntervention>, QrResult<List<TechnicalIntervention>, QrError>> { QrResult.Success(it) }
            .catch { e -> emit(QrResult.Error(QrError.InterventionError.LoadError(e.message))) }

    /**
     * Get interventions by specific status
     *
     * @param status InterventionStatus to filter by
     * @return Flow with QrResult containing filtered interventions
     */
    fun getInterventionsByStatus(status: InterventionStatus): Flow<QrResult<List<TechnicalIntervention>, QrError>> =
        interventionRepository.getInterventionsByStatusFlow(status)
            .map<List<TechnicalIntervention>, QrResult<List<TechnicalIntervention>, QrError>> { interventions ->
                QrResult.Success(interventions.sortedByDescending { it.updatedAt })
            }
            .catch { e -> emit(QrResult.Error(QrError.InterventionError.LoadError(e.message))) }

    /**
     * Get completed interventions (COMPLETED + ARCHIVED)
     *
     * @return Flow with QrResult containing completed interventions
     */
    fun getCompletedInterventions(): Flow<QrResult<List<TechnicalIntervention>, QrError>> =
        interventionRepository.getCompletedInterventionsFlow()
            .map<List<TechnicalIntervention>, QrResult<List<TechnicalIntervention>, QrError>> { QrResult.Success(it) }
            .catch { e -> emit(QrResult.Error(QrError.InterventionError.LoadError(e.message))) }
}
