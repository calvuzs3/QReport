package net.calvuz.qreport.ti.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
import javax.inject.Inject

/**
 * Use Case: Update Technical Intervention Status
 *
 * Handles status transitions with business logic validation.
 * Supports both single and batch operations.
 */
class UpdateTechnicalInterventionStatusUseCase @Inject constructor(
    private val interventionRepository: TechnicalInterventionRepository
) {

    /**
     * Update status for single intervention
     *
     * @param interventionId ID of intervention to update
     * @param newStatus New status to set
     * @param debugMode If true, bypasses business rule validation
     * @return QrResult with updated TechnicalIntervention or error
     */
    suspend operator fun invoke(
        interventionId: String,
        newStatus: InterventionStatus,
        debugMode: Boolean = false
    ): QrResult<TechnicalIntervention, QrError> {

        // Validate input
        if (interventionId.isBlank()) {
            return QrResult.Error(QrError.InterventionError.INVALID_ID())
        }

        try {
            // Get current intervention
            val getResult = interventionRepository.getInterventionById(interventionId)

            if (getResult.isFailure) {
                return QrResult.Error(QrError.InterventionError.NOT_FOUND())
            }

            val intervention = getResult.getOrThrow()

            // Business rules validation (unless debug mode)
            if (!debugMode) {
                val validationResult = validateStatusTransition(intervention.status, newStatus)
                if (validationResult is QrResult.Error) {
                    return QrResult.Error(validationResult.error)
                }
            }

            // Update intervention with new status
            val updatedIntervention = intervention.copy(status = newStatus)

            val updateResult = interventionRepository.updateIntervention(updatedIntervention)

            return if (updateResult.isSuccess) {
                QrResult.Success(updatedIntervention)
            } else {
                val exception = updateResult.exceptionOrNull()
                QrResult.Error(QrError.InterventionError.UPDATE_FAILED(exception?.message))
            }

        } catch (e: Exception) {
            return QrResult.Error(QrError.InterventionError.UPDATE_FAILED(e.message))
        }
    }

    /**
     * Update status for multiple interventions (Batch operation)
     *
     * @param interventionIds List of intervention IDs to update
     * @param newStatus New status to set for all
     * @param debugMode If true, bypasses business rule validation
     * @return QrResult with update summary
     */
    suspend fun updateStatusBatch(
        interventionIds: List<String>,
        newStatus: InterventionStatus,
        debugMode: Boolean = false
    ): QrResult<StatusUpdateSummary, QrError> {

        if (interventionIds.isEmpty()) {
            return QrResult.Success(StatusUpdateSummary(0, 0, emptyList(), newStatus))
        }

        val errors = mutableListOf<String>()
        var successCount = 0
        var failureCount = 0

        interventionIds.forEach { interventionId ->
            when (val result = invoke(interventionId, newStatus, debugMode)) {
                is QrResult.Success -> successCount++
                is QrResult.Error -> {
                    failureCount++
                    errors.add("ID $interventionId: ${result.error}")
                }
            }
        }

        val summary = StatusUpdateSummary(successCount, failureCount, errors, newStatus)

        return if (failureCount == 0) {
            QrResult.Success(summary)
        } else if (successCount > 0) {
            // Partial success - return success with error details
            QrResult.Success(summary)
        } else {
            // Complete failure
            QrResult.Error(QrError.InterventionError.BATCH_UPDATE_FAILED(errors.firstOrNull()))
        }
    }

    /**
     * Set intervention to active status (IN_PROGRESS)
     */
    suspend fun setActive(interventionId: String): QrResult<TechnicalIntervention, QrError> {
        return invoke(interventionId, InterventionStatus.IN_PROGRESS, debugMode = false)
    }

    /**
     * Set intervention to inactive status (ARCHIVED)
     */
    suspend fun setInactive(interventionId: String): QrResult<TechnicalIntervention, QrError> {
        return invoke(interventionId, InterventionStatus.ARCHIVED, debugMode = false)
    }

    /**
     * Set multiple interventions to active status
     */
    suspend fun setActiveBatch(interventionIds: List<String>): QrResult<StatusUpdateSummary, QrError> {
        return updateStatusBatch(interventionIds, InterventionStatus.IN_PROGRESS, debugMode = false)
    }

    /**
     * Set multiple interventions to inactive status
     */
    suspend fun setInactiveBatch(interventionIds: List<String>): QrResult<StatusUpdateSummary, QrError> {
        return updateStatusBatch(interventionIds, InterventionStatus.ARCHIVED, debugMode = false)
    }

    /**
     * Validate business rules for status transitions
     */
    private fun validateStatusTransition(
        currentStatus: InterventionStatus,
        newStatus: InterventionStatus
    ): QrResult<Unit, QrError> {

        // Allow same status (no-op)
        if (currentStatus == newStatus) {
            return QrResult.Success(Unit)
        }

        // Define valid transitions
        val validTransitions = when (currentStatus) {
            InterventionStatus.DRAFT -> listOf(
                InterventionStatus.IN_PROGRESS,
                InterventionStatus.ARCHIVED  // Can archive drafts
            )
            InterventionStatus.IN_PROGRESS -> listOf(
                InterventionStatus.PENDING_REVIEW,
                InterventionStatus.COMPLETED,
                InterventionStatus.DRAFT,      // Can revert to draft
                InterventionStatus.ARCHIVED    // Can archive in-progress
            )
            InterventionStatus.PENDING_REVIEW -> listOf(
                InterventionStatus.IN_PROGRESS, // Back to work
                InterventionStatus.COMPLETED,   // Approved
                InterventionStatus.DRAFT        // Rejected, back to draft
            )
            InterventionStatus.COMPLETED -> listOf(
                InterventionStatus.ARCHIVED     // Only can archive completed
            )
            InterventionStatus.ARCHIVED -> listOf(
                InterventionStatus.IN_PROGRESS  // Can reactivate archived
            )
        }

        return if (newStatus in validTransitions) {
            QrResult.Success(Unit)
        } else {
            QrResult.Error(QrError.InterventionError.INVALID_STATUS_TRANSITION(currentStatus, newStatus))
        }
    }
}

/**
 * Status update operation summary
 */
data class StatusUpdateSummary(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>,
    val targetStatus: InterventionStatus
) {
    val totalCount: Int get() = successCount + failureCount
    val isCompleteSuccess: Boolean get() = failureCount == 0 && successCount > 0
    val isPartialSuccess: Boolean get() = failureCount > 0 && successCount > 0
    val isCompleteFailure: Boolean get() = successCount == 0 && failureCount > 0
}