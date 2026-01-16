package net.calvuz.qreport.ti.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
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
            return QrResult.Error(QrError.InterventionError.INVALID_ID())
        }

        try {
            // Get existing intervention to preserve immutable fields
            val existingResult = interventionRepository.getInterventionById(intervention.id)

            if (existingResult.isFailure) {
                return QrResult.Error(QrError.InterventionError.NOT_FOUND())
            }

            val existing = existingResult.getOrThrow()

            // Create updated intervention preserving immutable fields for fiscal compliance
            val updatedIntervention = intervention.copy(
                // Preserve immutable fields (fiscal compliance)
                interventionNumber = existing.interventionNumber, // Immutable
                createdAt = existing.createdAt, // Immutable

                // Update updatedAt timestamp
                updatedAt = Clock.System.now(),

                // Preserve critical customer data for fiscal compliance
                customerData = existing.customerData.copy(
                    // Allow only some customer fields to be updated
                    customerContact = intervention.customerData.customerContact,
                    notes = intervention.customerData.notes
                    // customerName, ticketNumber, customerOrderNumber remain immutable
                ),

                // Preserve robot serial number for fiscal compliance
                robotData = intervention.robotData.copy(
                    serialNumber = existing.robotData.serialNumber // Immutable
                    // hoursOfDuty can be updated
                )
            )

            // Validate business rules
            val validationResult = validateInterventionUpdate(existing, updatedIntervention)
            if (validationResult is QrResult.Error) {
                return QrResult.Error(validationResult.error)
            }

            // Perform update
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
            return QrResult.Error(QrError.InterventionError.INVALID_ID())
        }

        if (technicians.size > 6) {
            return QrResult.Error(QrError.CreateInterventionError.TooManyTechnicians())
        }

        try {
            // Get existing intervention
            val existingResult = interventionRepository.getInterventionById(interventionId)

            if (existingResult.isFailure) {
                return QrResult.Error(QrError.InterventionError.NOT_FOUND())
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
                QrResult.Error(QrError.InterventionError.UPDATE_FAILED(exception?.message))
            }

        } catch (e: Exception) {
            return QrResult.Error(QrError.InterventionError.UPDATE_FAILED(e.message))
        }
    }

    /**
     * Validate business rules for intervention updates
     */
    private fun validateInterventionUpdate(
        existing: TechnicalIntervention,
        updated: TechnicalIntervention
    ): QrResult<Unit, QrError> {

        // Check that critical immutable fields haven't changed
        if (existing.interventionNumber != updated.interventionNumber) {
            return QrResult.Error(QrError.InterventionError.IMMUTABLE_FIELD_CHANGED("interventionNumber"))
        }

        if (existing.customerData.customerName != updated.customerData.customerName) {
            return QrResult.Error(QrError.InterventionError.IMMUTABLE_FIELD_CHANGED("customerName"))
        }

        if (existing.customerData.ticketNumber != updated.customerData.ticketNumber) {
            return QrResult.Error(QrError.InterventionError.IMMUTABLE_FIELD_CHANGED("ticketNumber"))
        }

        if (existing.customerData.customerOrderNumber != updated.customerData.customerOrderNumber) {
            return QrResult.Error(QrError.InterventionError.IMMUTABLE_FIELD_CHANGED("customerOrderNumber"))
        }

        if (existing.robotData.serialNumber != updated.robotData.serialNumber) {
            return QrResult.Error(QrError.InterventionError.IMMUTABLE_FIELD_CHANGED("serialNumber"))
        }

        // Validate technicians count
        if (updated.technicians.size > 6) {
            return QrResult.Error(QrError.CreateInterventionError.TooManyTechnicians())
        }

        // Validate status transition (can't update status through this use case)
        if (existing.status != updated.status) {
            return QrResult.Error(QrError.InterventionError.STATUS_UPDATE_NOT_ALLOWED())
        }

        return QrResult.Success(Unit)
    }
}