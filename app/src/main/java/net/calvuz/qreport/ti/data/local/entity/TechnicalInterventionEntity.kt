package net.calvuz.qreport.ti.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.datetime.Instant
import net.calvuz.qreport.ti.data.conterter.TechnicalInterventionTypeConverters
import net.calvuz.qreport.ti.domain.model.InterventionStatus

/**
 * Room Entity for TechnicalIntervention fiscal documents
 */
@Entity(
    tableName = "technical_interventions",
    indices = [
        Index(value = ["intervention_number"], unique = true),
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["customer_name"])  // For search functionality
    ]
)
@TypeConverters(
    TechnicalInterventionTypeConverters::class
)
data class TechnicalInterventionEntity(
    @PrimaryKey
    val id: String,

    // ===== DOCUMENT METADATA =====
    val intervention_number: String,
    val created_at: Instant,
    val updated_at: Instant,
    val status: InterventionStatus,

    // ===== CUSTOMER SECTION (Stored as JSON for immutability) =====
    val customer_data: String,              // CustomerData as JSON

    // ===== ROBOT DATA SECTION =====
    val robot_data: String,                 // RobotData as JSON

    // ===== WORK LOCATION SECTION =====
    val work_location: String,              // WorkLocation as JSON

    // ===== TECHNICIAN SECTION =====
    val technicians: String,                // List<String> as JSON

    // ===== COMPLEX SECTIONS (Future expansion, stored as JSON) =====
    val work_days: String = "[]",           // List<WorkDay> as JSON
    val intervention_description: String = "",
    val materials_used: String? = null,     // MaterialsUsed as JSON
    val external_report: String? = null,    // ExternalReport as JSON
    val is_complete: Boolean = false,
    val technician_signature: String? = null, // TechnicianSignature as JSON
    val customer_signature: String? = null,   // CustomerSignature as JSON

    // ===== SEARCH HELPERS =====
    val customer_name: String               // Extracted from customer_data for indexing/search
)