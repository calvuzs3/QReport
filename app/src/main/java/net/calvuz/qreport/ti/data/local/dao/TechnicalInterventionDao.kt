package net.calvuz.qreport.ti.data.local.dao

import androidx.room.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.calvuz.qreport.ti.data.local.entity.TechnicalInterventionEntity
import net.calvuz.qreport.ti.domain.model.*

/**
 * Domain to Entity mappers
 */
fun TechnicalIntervention.toEntity(): TechnicalInterventionEntity {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    return TechnicalInterventionEntity(
        id = id,
        intervention_number = interventionNumber,
        created_at = createdAt,
        updated_at = updatedAt,
        status = status,
        customer_data = json.encodeToString(customerData),
        robot_data = json.encodeToString(robotData),
        work_location = json.encodeToString(workLocation),
        technicians = json.encodeToString(technicians),
        work_days = json.encodeToString(workDays),
        intervention_description = interventionDescription,
        materials_used = materials?.let { json.encodeToString(it) },
        external_report = externalReport?.let { json.encodeToString(it) },
        is_complete = isComplete,
        technician_signature = technicianSignature?.let { json.encodeToString(it) },
        customer_signature = customerSignature?.let { json.encodeToString(it) },
        customer_name = customerData.customerName // Extract for search indexing
    )
}

/**
 * Entity to Domain mappers
 */
fun TechnicalInterventionEntity.toDomain(): TechnicalIntervention {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    return TechnicalIntervention(
        id = id,
        interventionNumber = intervention_number,
        createdAt = created_at,
        updatedAt = updated_at,
        status = status,
        customerData = json.decodeFromString(customer_data),
        robotData = json.decodeFromString(robot_data),
        workLocation = json.decodeFromString(work_location),
        technicians = json.decodeFromString(technicians),
        workDays = json.decodeFromString(work_days),
        interventionDescription = intervention_description,
        materials = materials_used?.let { json.decodeFromString(it) },
        externalReport = external_report?.let { json.decodeFromString(it) },
        isComplete = is_complete,
        technicianSignature = technician_signature?.let { json.decodeFromString(it) },
        customerSignature = customer_signature?.let { json.decodeFromString(it) }
    )
}

/**
 * DAO for TechnicalIntervention operations
 */
@Dao
interface TechnicalInterventionDao {

    // ===== BASIC CRUD =====
    @Query("SELECT * FROM technical_interventions ORDER BY created_at DESC")
    suspend fun getAllInterventions(): List<TechnicalInterventionEntity>

    @Query("SELECT * FROM technical_interventions WHERE id = :id")
    suspend fun getInterventionById(id: String): TechnicalInterventionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: TechnicalInterventionEntity)

    @Update
    suspend fun updateIntervention(intervention: TechnicalInterventionEntity)

    @Delete
    suspend fun deleteIntervention(intervention: TechnicalInterventionEntity)

    // ===== ADVANCED CRUD =====
    @Query("DELETE FROM technical_interventions WHERE id = :id")
    suspend fun deleteInterventionById(id: String)


    // ===== QUERIES BY STATUS =====
    @Query("SELECT * FROM technical_interventions WHERE status = :status ORDER BY created_at DESC")
    suspend fun getInterventionsByStatus(status: InterventionStatus): List<TechnicalInterventionEntity>

    @Query("SELECT * FROM technical_interventions WHERE status IN ('DRAFT', 'IN_PROGRESS') ORDER BY created_at DESC")
    suspend fun getActiveInterventions(): List<TechnicalInterventionEntity>

    @Query("SELECT * FROM technical_interventions WHERE status = 'COMPLETED' ORDER BY created_at DESC")
    suspend fun getCompletedInterventions(): List<TechnicalInterventionEntity>

    // ===== SEARCH =====
    @Query("""
        SELECT * FROM technical_interventions 
        WHERE customer_data LIKE '%' || :customerName || '%' 
        OR intervention_number LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    suspend fun searchInterventions(customerName: String = "", query: String = ""): List<TechnicalInterventionEntity>

    // ===== STATISTICS =====
    @Query("SELECT COUNT(*) FROM technical_interventions WHERE status = :status")
    suspend fun countByStatus(status: InterventionStatus): Int

    @Query("SELECT COUNT(*) FROM technical_interventions WHERE is_complete = 1")
    suspend fun countCompleted(): Int

    // ===== UTILITY =====
    @Query("SELECT intervention_number FROM technical_interventions ORDER BY intervention_number DESC LIMIT 1")
    suspend fun getLastInterventionNumber(): String?
}