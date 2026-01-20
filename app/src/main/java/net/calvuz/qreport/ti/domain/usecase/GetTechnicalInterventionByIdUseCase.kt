package net.calvuz.qreport.ti.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
import javax.inject.Inject

/**
 * Use Case: Get Technical Intervention by ID
 *
 * Used for loading existing intervention data in edit mode.
 */
class GetTechnicalInterventionByIdUseCase @Inject constructor(
    private val interventionRepository: TechnicalInterventionRepository
) {

    /**
     * Get technical intervention by ID
     *
     * @param interventionId ID of intervention to retrieve
     * @return QrResult with TechnicalIntervention or error
     */
    suspend operator fun invoke(interventionId: String): QrResult<TechnicalIntervention, QrError> {
        // Validate input
        if (interventionId.isBlank()) {
            return QrResult.Error(QrError.InterventionError.InvalidId())
        }

        return try {
            val result = interventionRepository.getInterventionById(interventionId)

            if (result.isSuccess) {
                val intervention = result.getOrThrow()
                QrResult.Success(intervention)
            } else {
                result.exceptionOrNull()
                QrResult.Error(QrError.InterventionError.NotFound())
            }

        } catch (e: Exception) {
            QrResult.Error(QrError.InterventionError.LoadError(e.message))
        }
    }
}