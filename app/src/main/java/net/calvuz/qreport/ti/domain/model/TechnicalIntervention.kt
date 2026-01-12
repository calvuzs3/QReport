package net.calvuz.qreport.ti.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Technical Intervention Document - Fiscal Document for External Services
 *
 * Separate from CheckUp, with immutable customer/robot data copies for fiscal compliance.
 * Priority implementation: Customer Section + Robot Data Section
 */
@Serializable
data class TechnicalIntervention(
    val id: String,

    // ===== DOCUMENT METADATA =====
    val interventionNumber: String,          // Auto-generated sequential number
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: InterventionStatus,

    // ===== CUSTOMER SECTION (Immutable copies for fiscal compliance) =====
    val customerData: CustomerData,

    // ===== ROBOT DATA SECTION =====
    val robotData: RobotData,

    // ===== WORK LOCATION SECTION (Simple implementation) =====
    val workLocation: WorkLocation,

    // ===== TECHNICIAN SECTION (Simple implementation) =====
    val technicians: List<String> = emptyList(),  // Up to 6 technician names

    // ===== FUTURE SECTIONS (Placeholder for expansion) =====
    val workDays: List<WorkDay> = emptyList(),              // Daily details table
    val interventionDescription: String = "",               // Technical description
    val materials: MaterialsUsed? = null,                  // DDT references + items
    val externalReport: ExternalReport? = null,            // External company report
    val isComplete: Boolean = false,                       // Intervention completion flag
    val technicianSignature: TechnicianSignature? = null, // Technician signature section
    val customerSignature: CustomerSignature? = null      // Customer signature section
)

/**
 * Customer data section - Immutable copy for fiscal document
 */
@Serializable
data class CustomerData(
    val customerName: String,                    // Required field *
    val customerContact: String = "",            // Reference person
    val notes: String = "",
    val ticketNumber: String,                    // Required field *
    val customerOrderNumber: String              // Required field *
)

/**
 * Robot data section
 */
@Serializable
data class RobotData(
    val serialNumber: String,                    // Required field *
    val hoursOfDuty: Int                        // Required field *
)

/**
 * Work location section - Choice between client site/our site/other
 */
@Serializable
data class WorkLocation(
    val type: WorkLocationType,
    val customLocation: String = ""             // Free text when type = OTHER
)

@Serializable
enum class WorkLocationType {
    CLIENT_SITE,     // Sede cliente
    OUR_SITE,        // Nostra sede
    OTHER           // Altro (requires customLocation)
}

/**
 * Daily work details (Future expansion - complex table)
 */
@Serializable
data class WorkDay(
    val date: Instant,
    val remoteAssistance: Boolean = false,      // Disables travel fields when true
    val technicianCount: Int,
    val technicianInitials: String = "",        // Comma separated initials

    // Travel and work hours
    val outboundTravelStart: String = "",       // HH:mm format
    val outboundTravelEnd: String = "",
    val morningStart: String = "",
    val morningEnd: String = "",
    val afternoonStart: String = "",
    val afternoonEnd: String = "",
    val returnTravelStart: String = "",
    val returnTravelEnd: String = "",

    // Expense flags
    val morningPocketMoney: Boolean = false,    // Flag pocket money 1/2 morning
    val afternoonPocketMoney: Boolean = false,  // Flag pocket money 1/2 afternoon
    val totalKilometers: Double = 0.0,
    val flight: Boolean = false,
    val rentCar: Boolean = false,
    val transferToAirport: Boolean = false,
    val lodging: Boolean = false
)

/**
 * Materials used section (Future expansion)
 */
@Serializable
data class MaterialsUsed(
    val ddtNumber: String = "",                 // DDT reference number
    val ddtDate: Instant? = null,              // DDT reference date
    val items: List<MaterialItem> = emptyList() // Up to 6 rows: quantity, description
)

@Serializable
data class MaterialItem(
    val quantity: Double,
    val description: String
)

/**
 * External company report section (Future expansion)
 */
@Serializable
data class ExternalReport(
    val reportNumber: String
)

/**
 * Technician signature section (Future expansion - placeholder)
 */
@Serializable
data class TechnicianSignature(
    val name: String,
    val signature: String = ""                  // Placeholder for future holographic signature
)

/**
 * Customer signature section (Future expansion - placeholder)
 */
@Serializable
data class CustomerSignature(
    val name: String,
    val signature: String = ""                  // Placeholder for future holographic signature via tablet
)

/**
 * Intervention document status
 */
@Serializable
enum class InterventionStatus {
    DRAFT,          // In preparazione
    IN_PROGRESS,    // In corso
    PENDING_REVIEW, // In attesa approvazione
    COMPLETED,      // Completato
    ARCHIVED        // Archiviato
}

/**
 * Factory methods to create TechnicalIntervention from existing entities
 */
object TechnicalInterventionFactory {

    /**
     * Create TechnicalIntervention from existing Client and Island (immutable copies)
     */
    fun createFromClientAndIsland(
        clientData: CustomerData,
        robotData: RobotData,
        workLocation: WorkLocation = WorkLocation(WorkLocationType.CLIENT_SITE),
        technicians: List<String> = emptyList()
    ): TechnicalIntervention {
        return TechnicalIntervention(
            id = generateInterventionId(),
            interventionNumber = generateInterventionNumber(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            status = InterventionStatus.DRAFT,
            customerData = clientData,
            robotData = robotData,
            workLocation = workLocation,
            technicians = technicians
        )
    }

    private fun generateInterventionId(): String = UUID.randomUUID().toString()

    private fun generateInterventionNumber(): String {
        val timestamp = Clock.System.now().epochSeconds
        return "INT-${timestamp}"
    }
}