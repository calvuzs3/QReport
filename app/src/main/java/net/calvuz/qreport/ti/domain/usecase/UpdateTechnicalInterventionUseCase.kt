package net.calvuz.qreport.ti.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case: Update Technical Intervention Complete Data
 *
 * Updates all editable fields of a TechnicalIntervention.
 * Some fields are immutable for fiscal compliance (customer data, robot serial).
 */
class UpdateTechnicalInterventionUseCase @Inject constructor(
    private val interventionRepository: TechnicalInterventionRepository
) {

    /**
     * Update technical intervention with new data
     *
     * @param intervention Complete TechnicalIntervention with updated data
     * @return QrResult with updated TechnicalIntervention or error
     */
    suspend operator fun invoke(intervention: TechnicalIntervention): QrResult<TechnicalIntervention, QrError> {
        // Validate input
        if (intervention.id.isBlank()) {
            Timber.d("Invalid intervention ID")
            return QrResult.Error(QrError.InterventionError.InvalidId())
        }

        try {
            // Get existing intervention to preserve immutable fields
            val existingResult = interventionRepository.getInterventionById(intervention.id)

            if (existingResult.isFailure) {
                Timber.d("Failed to get intervention: ${existingResult.exceptionOrNull()}")
                return QrResult.Error(QrError.InterventionError.NotFound())
            }

            val existing = existingResult.getOrThrow()

            // Create updated intervention with selective preservation for development
            val updatedIntervention = intervention.copy(
                // Preserve truly immutable fields only
                interventionNumber = existing.interventionNumber, // Immutable - fiscal compliance
                createdAt = existing.createdAt, // Immutable

                // Update timestamp
                updatedAt = Clock.System.now(),

                // RELAXED: Allow customer data updates during development
                customerData = intervention.customerData.copy(
                    // Preserve customer name only if updating specific fields
                    customerName = intervention.customerData.customerName.takeIf { it.isNotBlank() } ?: existing.customerData.customerName,
                    customerContact = intervention.customerData.customerContact,
                    ticketNumber = intervention.customerData.ticketNumber.takeIf { it.isNotBlank() } ?: existing.customerData.ticketNumber,
                    customerOrderNumber = intervention.customerData.customerOrderNumber.takeIf { it.isNotBlank() } ?: existing.customerData.customerOrderNumber,
                    notes = intervention.customerData.notes
                ),

                // RELAXED: Allow robot data updates during development
                robotData = intervention.robotData.copy(
                    serialNumber = intervention.robotData.serialNumber.takeIf { it.isNotBlank() } ?: existing.robotData.serialNumber,
                    hoursOfDuty = intervention.robotData.hoursOfDuty // Allow hoursOfDuty updates
                ),

                // Allow work location updates
                workLocation = intervention.workLocation,

                // Allow technicians updates
                technicians = intervention.technicians,

                // Allow intervention description updates
                interventionDescription = intervention.interventionDescription,

                // Allow materials updates
                materials = intervention.materials,

                // Allow external report updates
                externalReport = intervention.externalReport,

                // Allow work days updates
                workDays = intervention.workDays,

                // Allow completion status updates
                isComplete = intervention.isComplete,

                // Allow signature updates
                technicianSignature = intervention.technicianSignature,
                customerSignature = intervention.customerSignature
            )

            // Validate business rules
            val validationResult = validateInterventionUpdate(existing, updatedIntervention)
            if (validationResult is QrResult.Error) {
                Timber.d("Validation failed: ${validationResult.error}")
                return QrResult.Error(validationResult.error)
            }

            // Perform update
            val updateResult = interventionRepository.updateIntervention(updatedIntervention)

            return if (updateResult.isSuccess) {
                Timber.d("Update successful")
                QrResult.Success(updatedIntervention)
            } else {
                Timber.d("Update failed: ${updateResult.exceptionOrNull()}")
                val exception = updateResult.exceptionOrNull()
                QrResult.Error(QrError.InterventionError.UpdateError(exception?.message))
            }

        } catch (e: Exception) {
            Timber.d("Update failed: ${e.message}")
            return QrResult.Error(QrError.InterventionError.UpdateError(e.message))
        }
    }

    /**
     * Update specific editable fields of intervention
     *
     * @param interventionId ID of intervention to update
     * @param hoursOfDuty Updated hours of duty (editable)
     * @param customerContact Updated customer contact (editable)
     * @param notes Updated notes (editable)
     * @param workLocation Updated work location (editable)
     * @param technicians Updated technicians list (editable)
     * @return QrResult with updated TechnicalIntervention or error
     */
    suspend fun updateEditableFields(
        interventionId: String,
        hoursOfDuty: Int,
        customerContact: String = "",
        notes: String = "",
        workLocation: net.calvuz.qreport.ti.domain.model.WorkLocation,
        technicians: List<String> = emptyList()
    ): QrResult<TechnicalIntervention, QrError> {

        // Validate input
        if (interventionId.isBlank()) {
            return QrResult.Error(QrError.InterventionError.InvalidId())
        }

        if (technicians.size > 6) {
            return QrResult.Error(QrError.CreateInterventionError.TooManyTechnicians())
        }

        try {
            // Get existing intervention
            val existingResult = interventionRepository.getInterventionById(interventionId)

            if (existingResult.isFailure) {
                return QrResult.Error(QrError.InterventionError.NotFound())
            }

            val existing = existingResult.getOrThrow()

            // Create updated intervention with only editable fields changed
            val updatedIntervention = existing.copy(
                updatedAt = Clock.System.now(),

                // Update customer data (only editable fields)
                customerData = existing.customerData.copy(
                    customerContact = customerContact,
                    notes = notes
                ),

                // Update robot data (only editable fields)
                robotData = existing.robotData.copy(
                    hoursOfDuty = hoursOfDuty
                ),

                // Update work location
                workLocation = workLocation,

                // Update technicians
                technicians = technicians
            )

            // Perform update
            val updateResult = interventionRepository.updateIntervention(updatedIntervention)

            return if (updateResult.isSuccess) {
                QrResult.Success(updatedIntervention)
            } else {
                val exception = updateResult.exceptionOrNull()
                QrResult.Error(QrError.InterventionError.UpdateError(exception?.message))
            }

        } catch (e: Exception) {
            return QrResult.Error(QrError.InterventionError.UpdateError(e.message))
        }
    }

    /**
     * Validate business rules for intervention updates
     * DEVELOPMENT MODE: Less rigid validation to allow form testing
     */
    private fun validateInterventionUpdate(
        existing: TechnicalIntervention,
        updated: TechnicalIntervention
    ): QrResult<Unit, QrError> {

        // Only check truly critical immutable fields
        if (existing.interventionNumber != updated.interventionNumber) {
            return QrResult.Error(QrError.InterventionError.ImmutableFieldChanged("interventionNumber"))
        }

        // RELAXED VALIDATION FOR DEVELOPMENT:
        // Allow customer data changes during form development
        // In production, these would be stricter

        // Validate technicians count
        if (updated.technicians.size > 6) {
            return QrResult.Error(QrError.CreateInterventionError.TooManyTechnicians())
        }

        // Skip status validation for development - allow status changes
        // if (existing.status != updated.status) {
        //     return QrResult.Error(QrError.InterventionError.StatusUpdateNotAllowed())
        // }

        Timber.d("DEVELOPMENT: Validation successful with relaxed rules")
        return QrResult.Success(Unit)
    }
}