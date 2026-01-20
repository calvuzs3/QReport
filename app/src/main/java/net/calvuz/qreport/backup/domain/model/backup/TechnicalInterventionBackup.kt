package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * TechnicalInterventionBackup - Backup of technical interventions
 *
 * Contains all TechnicalIntervention data serialized for backup.
 * Signature file paths are stored here; actual files go in signatures.zip
 */
@Serializable
data class TechnicalInterventionBackup(
    val id: String,
    val interventionNumber: String,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    val status: String,

    // Customer section (JSON serialized)
    val customerDataJson: String,

    // Robot section (JSON serialized)
    val robotDataJson: String,

    // Work location (JSON serialized)
    val workLocationJson: String,

    // Technicians (JSON serialized list)
    val techniciansJson: String,

    // Work days (JSON serialized list)
    val workDaysJson: String,

    // Description
    val interventionDescription: String,

    // Materials (nullable JSON)
    val materialsUsedJson: String?,

    // External report (nullable JSON)
    val externalReportJson: String?,

    // Completion flag
    val isComplete: Boolean,

    // Signatures (nullable JSON with file path reference)
    val technicianSignatureJson: String?,
    val customerSignatureJson: String?,

    // Indexed field for search
    val customerName: String
)