package net.calvuz.qreport.ti.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
import javax.inject.Inject

/**
 * Use Case: Delete Technical Intervention
 *
 * Business Rules:
 * - Can delete DRAFT interventions without restrictions
 * - Can delete IN_PROGRESS with warning
 * - Cannot delete COMPLETED or ARCHIVED interventions
 * - PENDING_REVIEW interventions require confirmation
 */
class DeleteTechnicalInterventionUseCase @Inject constructor(
    private val interventionRepository: TechnicalInterventionRepository
) {

    /**
     * Delete single technical intervention
     *
     * @param interventionId ID of intervention to delete
     * @param forceDelete Override business rules (for admin/debug mode)
     * @return QrResult with Unit on success or error
     */
    suspend operator fun invoke(
        interventionId: String,
        forceDelete: Boolean = false
    ): QrResult<Unit, QrError> {

        // Validate input
        if (interventionId.isBlank()) {
            return QrResult.Error(QrError.InterventionError.INVALID_ID())
        }

        try {
            // Get intervention to validate business rules
            val getResult = interventionRepository.getInterventionById(interventionId)

            if (getResult.isFailure) {
                return QrResult.Error(QrError.InterventionError.NOT_FOUND())
            }

            val intervention = getResult.getOrThrow()

            // Business rules validation (unless force delete)
            if (!forceDelete) {
                when (intervention.status) {
                    InterventionStatus.COMPLETED -> {
                        return QrResult.Error(QrError.InterventionError.CANNOT_DELETE_COMPLETED())
                    }
                    InterventionStatus.ARCHIVED -> {
                        return QrResult.Error(QrError.InterventionError.CANNOT_DELETE_ARCHIVED())
                    }
                    InterventionStatus.PENDING_REVIEW -> {
                        return QrResult.Error(QrError.InterventionError.DELETE_REQUIRES_CONFIRMATION())
                    }
                    InterventionStatus.DRAFT,
                    InterventionStatus.IN_PROGRESS -> {
                        // OK to delete
                    }
                }
            }

            // Perform deletion
            val deleteResult = interventionRepository.deleteIntervention(interventionId)

            return if (deleteResult.isSuccess) {
                QrResult.Success(Unit)
            } else {
                val exception = deleteResult.exceptionOrNull()
                QrResult.Error(QrError.InterventionError.DELETE_FAILED(exception?.message))
            }

        } catch (e: Exception) {
            return QrResult.Error(QrError.InterventionError.DELETE_FAILED(e.message))
        }
    }

    /**
     * Delete multiple interventions with batch validation
     *
     * @param interventionIds List of intervention IDs to delete
     * @param forceDelete Override business rules for all
     * @return QrResult with deletion summary
     */
    suspend fun deleteMultiple(
        interventionIds: List<String>,
        forceDelete: Boolean = false
    ): QrResult<DeletionSummary, QrError> {

        if (interventionIds.isEmpty()) {
            return QrResult.Success(DeletionSummary(0, 0, emptyList()))
        }

        val errors = mutableListOf<String>()
        var successCount = 0
        var failureCount = 0

        interventionIds.forEach { interventionId ->
            when (val result = invoke(interventionId, forceDelete)) {
                is QrResult.Success -> successCount++
                is QrResult.Error -> {
                    failureCount++
                    errors.add("ID $interventionId: ${result.error}")
                }
            }
        }

        val summary = DeletionSummary(successCount, failureCount, errors)

        return if (failureCount == 0) {
            QrResult.Success(summary)
        } else if (successCount > 0) {
            // Partial success - return success with error details
            QrResult.Success(summary)
        } else {
            // Complete failure
            QrResult.Error(QrError.InterventionError.BATCH_DELETE_FAILED(errors.firstOrNull()))
        }
    }
}

/**
 * Deletion operation summary
 */
data class DeletionSummary(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
) {
    val totalCount: Int get() = successCount + failureCount
    val isCompleteSuccess: Boolean get() = failureCount == 0 && successCount > 0
    val isPartialSuccess: Boolean get() = failureCount > 0 && successCount > 0
    val isCompleteFailure: Boolean get() = successCount == 0 && failureCount > 0
}