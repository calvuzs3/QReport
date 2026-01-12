package net.calvuz.qreport.domain.usecase.intervention

import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.domain.model.QrError.CreateInterventionError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.repository.ClientRepository
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.ti.domain.model.CustomerData
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.model.RobotData
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.model.WorkLocation
import net.calvuz.qreport.ti.domain.model.WorkLocationType
import net.calvuz.qreport.ti.domain.repository.TechnicalInterventionRepository
import javax.inject.Inject

/**
 * Use Case: Create TechnicalIntervention from existing Client and Island
 *
 * Creates immutable copies of client/island data for fiscal compliance.
 * Priority implementation: Customer Data + Robot Data sections only.
 */
class CreateTechnicalInterventionUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val islandRepository: IslandRepository,
    private val interventionRepository: TechnicalInterventionRepository
) {

    /**
     * Create TechnicalIntervention from Client and Island entities
     *
     * @param clientId Source client for immutable customer data copy
     * @param islandId Source island for immutable robot data copy
     * @param ticketNumber Required ticket/job number
     * @param customerOrderNumber Required customer order number
     * @param workLocation Work location type (default: CLIENT_SITE)
     * @param technicians List of technician names (max 6)
     * @return Created TechnicalIntervention ID or error
     */
    suspend operator fun invoke(
        clientId: String,
        islandId: String,
        ticketNumber: String,
        customerOrderNumber: String,
        workLocation: WorkLocation = WorkLocation(WorkLocationType.CLIENT_SITE),
        technicians: List<String> = emptyList()
    ): QrResult<String, QrError> {

        // Validate required fields
        if (ticketNumber.isBlank()) {
            return QrResult.Error(CreateInterventionError.MissingTicketNumber())
        }

        if (customerOrderNumber.isBlank()) {
            return QrResult.Error(CreateInterventionError.MissingOrderNumber())
        }

        if (technicians.size > 6) {
            return QrResult.Error(CreateInterventionError.TooManyTechnicians())
        }

        try {
            // Get source client data - uses Result<T> from Kotlin
            val clientResult = clientRepository.getClientById(clientId)
            val client = clientResult.getOrNull()
                ?: return QrResult.Error(CreateInterventionError.ClientNotFound())

            // Get primary contact - uses QrResult<T, QrError>
            val primaryContact = when (val contactResult = clientRepository.getPrimaryContact(clientId)) {
                is QrResult.Success -> contactResult.data
                is QrResult.Error -> null // Contact is optional, continue without error
            }

            // Get source island data - uses Result<T> from Kotlin
            val islandResult = islandRepository.getIslandById(islandId)
            val island = islandResult.getOrNull()
                ?: return QrResult.Error(CreateInterventionError.IslandNotFound())

            // Create immutable customer data copy
            val customerData = CustomerData(
                customerName = client.companyName,  // Required field *
                customerContact = primaryContact?.fullName ?: "", // Reference person
                notes = "", // Empty by default, can be filled later
                ticketNumber = ticketNumber,        // Required field *
                customerOrderNumber = customerOrderNumber // Required field *
            )

            // Create immutable robot data copy
            val robotData = RobotData(
                serialNumber = island.serialNumber, // Required field *
                hoursOfDuty = island.operatingHours // Required field *
            )

            // Generate new intervention number
            val interventionNumber = generateInterventionNumber()

            // Create TechnicalIntervention
            val intervention = TechnicalIntervention(
                id = generateInterventionId(),
                interventionNumber = interventionNumber,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                status = InterventionStatus.DRAFT,
                customerData = customerData,
                robotData = robotData,
                workLocation = workLocation,
                technicians = technicians
            )

            // Persist intervention - uses Result<T> from Kotlin
            val createResult = interventionRepository.createIntervention(intervention)
            return if (createResult.isSuccess) {
                QrResult.Success(intervention.id)
            } else {
                QrResult.Error(CreateInterventionError.CreationFailed((intervention.toString())))
            }

        } catch (e: Exception) {
            return QrResult.Error(CreateInterventionError.CreationFailed(e.message))
        }
    }

    /**
     * Create TechnicalIntervention with manual data entry (no source entities)
     *
     * For cases where client/island data is not in the system
     */
    suspend fun createWithManualData(
        customerName: String,
        serialNumber: String,
        hoursOfDuty: Int,
        ticketNumber: String,
        customerOrderNumber: String,
        customerContact: String = "",
        workLocation: WorkLocation = WorkLocation(WorkLocationType.CLIENT_SITE),
        technicians: List<String> = emptyList()
    ): QrResult<String, QrError> {

        // Validate required fields
        if (customerName.isBlank()) {
            return QrResult.Error(CreateInterventionError.MissingCustomerName())
        }

        if (serialNumber.isBlank()) {
            return QrResult.Error(CreateInterventionError.MissingSerialNumber())
        }

        if (ticketNumber.isBlank()) {
            return QrResult.Error(CreateInterventionError.MissingSerialNumber())
        }

        if (customerOrderNumber.isBlank()) {
            return QrResult.Error(CreateInterventionError.MissingOrderNumber())
        }

        if (technicians.size > 6) {
            return QrResult.Error(CreateInterventionError.TooManyTechnicians())
        }

        try {
            // Create customer data
            val customerData = CustomerData(
                customerName = customerName,
                customerContact = customerContact,
                notes = "",
                ticketNumber = ticketNumber,
                customerOrderNumber = customerOrderNumber
            )

            // Create robot data
            val robotData = RobotData(
                serialNumber = serialNumber,
                hoursOfDuty = hoursOfDuty
            )

            // Generate new intervention number
            val interventionNumber = generateInterventionNumber()

            // Create TechnicalIntervention
            val intervention = TechnicalIntervention(
                id = generateInterventionId(),
                interventionNumber = interventionNumber,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                status = InterventionStatus.DRAFT,
                customerData = customerData,
                robotData = robotData,
                workLocation = workLocation,
                technicians = technicians
            )

            // Persist intervention - uses Result<T> from Kotlin
            val createResult = interventionRepository.createIntervention(intervention)
            return if (createResult.isSuccess) {
                QrResult.Success(intervention.id)
            } else {
                QrResult.Error(CreateInterventionError.CreationFailed((intervention.toString())))
            }

        } catch (e: Exception) {
            return QrResult.Error(CreateInterventionError.CreationFailed(e.message))
        }
    }

    private fun generateInterventionId(): String = java.util.UUID.randomUUID().toString()

    private suspend fun generateInterventionNumber(): String {
        // Handle Result<T> from getLastInterventionNumber
        val lastNumber = interventionRepository.getLastInterventionNumber().getOrNull()

        val sequence = if (lastNumber != null) {
            val lastSequence = lastNumber.substringAfter("INT-").toIntOrNull() ?: 0
            lastSequence + 1
        } else {
            1
        }
        return "INT-${sequence.toString().padStart(6, '0')}"
    }
}

/**
 * Specific errors for TechnicalIntervention creation following QrError pattern
 */
//sealed interface CreateInterventionError : QrError {
//    data object ClientNotFound : CreateInterventionError
//    data object IslandNotFound : CreateInterventionError
//    data object MissingCustomerName : CreateInterventionError
//    data object MissingSerialNumber : CreateInterventionError
//    data object MissingTicketNumber : CreateInterventionError
//    data object MissingOrderNumber : CreateInterventionError
//    data object TooManyTechnicians : CreateInterventionError
//    data class CreationFailed(val exception: Exception? = null) : CreateInterventionError
//}

/**
 * Extension for Client repository to get primary contact (returns QrResult)
 */
suspend fun ClientRepository.getPrimaryContact(clientId: String): QrResult<Contact?, QrError> {
    // Implementation depends on existing ClientRepository
    // This is a placeholder - should be implemented based on actual Client architecture
    // For now, return empty success to indicate no contact found (optional field)
    return QrResult.Success(null)
}

/**
 * Extension for Island repository access (returns Result<T>)
 */
//interface IslandRepository {
//    suspend fun getIslandById(id: String): Result<Island>
//}

/**
 * Note: ClientRepository.getClientById should return Result<Client>
 * This assumes the existing ClientRepository already follows this pattern
 */