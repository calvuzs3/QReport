package net.calvuz.qreport.backup.data

import net.calvuz.qreport.backup.domain.model.backup.TechnicalInterventionBackup
import net.calvuz.qreport.ti.data.local.entity.TechnicalInterventionEntity
import net.calvuz.qreport.ti.domain.model.InterventionStatus

/**
 * Extension function to convert TechnicalInterventionEntity to TechnicalInterventionBackup
 *
 * Follows the same pattern as other entity mappers in DatabaseExporter.kt
 */
fun TechnicalInterventionEntity.toBackup(): TechnicalInterventionBackup {
    return TechnicalInterventionBackup(
        id = id,
        interventionNumber = intervention_number,
        createdAt = created_at,
        updatedAt = updated_at,
        status = status.name,
        customerDataJson = customer_data,
        robotDataJson = robot_data,
        workLocationJson = work_location,
        techniciansJson = technicians,
        workDaysJson = work_days,
        interventionDescription = intervention_description,
        materialsUsedJson = materials_used,
        externalReportJson = external_report,
        isComplete = is_complete,
        technicianSignatureJson = technician_signature,
        customerSignatureJson = customer_signature,
        customerName = customer_name
    )
}


/**
 * Extension function to convert TechnicalInterventionBackup to TechnicalInterventionEntity
 *
 * Reverse of TechnicalInterventionEntity.toBackup()
 * Used during restore/import operations
 */
fun TechnicalInterventionBackup.toEntity(): TechnicalInterventionEntity {
    return TechnicalInterventionEntity(
        id = id,
        intervention_number = interventionNumber,
        created_at = createdAt,
        updated_at = updatedAt,
        status = InterventionStatus.valueOf(status),
        customer_data = customerDataJson,
        robot_data = robotDataJson,
        work_location = workLocationJson,
        technicians = techniciansJson,
        work_days = workDaysJson,
        intervention_description = interventionDescription,
        materials_used = materialsUsedJson,
        external_report = externalReportJson,
        is_complete = isComplete,
        technician_signature = technicianSignatureJson,
        customer_signature = customerSignatureJson,
        customer_name = customerName
    )
}