package net.calvuz.qreport.domain.repository

import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface per gestione stabilimenti
 * Definisce il contratto per accesso ai dati degli stabilimenti
 * Implementazione nel data layer
 */
interface FacilityRepository {

    // ===== CRUD OPERATIONS =====

    suspend fun getAllFacilities(): Result<List<Facility>>
    suspend fun getActiveFacilities(): Result<List<Facility>>
    suspend fun getFacilityById(id: String): Result<Facility?>
    suspend fun createFacility(facility: Facility): Result<Unit>
    suspend fun updateFacility(facility: Facility): Result<Unit>
    suspend fun deleteFacility(id: String): Result<Unit>

    // ===== CLIENT RELATED =====

    suspend fun getFacilitiesByClient(clientId: String): Result<List<Facility>>
    fun getFacilitiesByClientFlow(clientId: String): Flow<List<Facility>>
    suspend fun getActiveFacilitiesByClient(clientId: String): Result<List<Facility>>
    suspend fun getPrimaryFacility(clientId: String): Result<Facility?>

    // ===== FLOW OPERATIONS (REACTIVE) =====

    fun getAllActiveFacilitiesFlow(): Flow<List<Facility>>
    fun getFacilityByIdFlow(id: String): Flow<Facility?>

    // ===== SEARCH & FILTER =====

    suspend fun searchFacilities(query: String): Result<List<Facility>>
    suspend fun getFacilitiesByType(facilityType: FacilityType): Result<List<Facility>>

    // ===== VALIDATION =====

    suspend fun isFacilityNameTakenForClient(
        clientId: String,
        name: String,
        excludeId: String = ""
    ): Result<Boolean>
    suspend fun hasPrimaryFacility(clientId: String, excludeId: String = ""): Result<Boolean>

    // ===== STATISTICS =====

    suspend fun getActiveFacilitiesCount(): Result<Int>
    suspend fun getFacilitiesCountByClient(clientId: String): Result<Int>
    suspend fun getIslandsCount(facilityId: String): Result<Int>
    suspend fun getAllFacilityTypes(): Result<List<FacilityType>>

    // ===== COMPLEX QUERIES =====

    suspend fun getFacilitiesWithIslands(): Result<List<Facility>>

    // ===== BULK OPERATIONS =====

    suspend fun createFacilities(facilities: List<Facility>): Result<Unit>
    suspend fun setPrimaryFacility(clientId: String, facilityId: String): Result<Unit>

    // ===== MAINTENANCE =====

    suspend fun touchFacility(id: String): Result<Unit>
}