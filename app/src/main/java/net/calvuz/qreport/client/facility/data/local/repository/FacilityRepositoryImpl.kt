package net.calvuz.qreport.client.facility.data.local.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.client.facility.data.local.dao.FacilityDao
import net.calvuz.qreport.client.facility.data.local.mapper.FacilityMapper
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.model.FacilityType
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Implementazione del repository per gestione stabilimenti
 * Utilizza Room DAO per persistenza e mapper per conversioni domain ↔ entity
 */
class FacilityRepositoryImpl @Inject constructor(
    private val facilityDao: FacilityDao,
    private val facilityMapper: FacilityMapper
) : FacilityRepository {

    // ===== CRUD OPERATIONS =====

    override suspend fun getAllFacilities(): Result<List<Facility>> {
        return try {
            val entities = facilityDao.getAllActiveFacilities() // Nome DAO corretto
            val facilities = facilityMapper.toDomainList(entities)
            Result.success(facilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveFacilities(): Result<List<Facility>> {
        return try {
            val entities = facilityDao.getAllActiveFacilities()
            val facilities = facilityMapper.toDomainList(entities)
            Result.success(facilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFacilityById(id: String): Result<Facility?> {
        return try {
            val entity = facilityDao.getFacilityById(id)
            val facility = entity?.let { facilityMapper.toDomain(it) }
            Result.success(facility)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFacility(facility: Facility): Result<Unit> {
        return try {
            val entity = facilityMapper.toEntity(facility)
            facilityDao.insertFacility(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFacility(facility: Facility): Result<Unit> {
        return try {
            val entity = facilityMapper.toEntity(facility)
            facilityDao.updateFacility(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFacility(id: String): Result<Unit> {
        return try {
            facilityDao.softDeleteFacility(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== CLIENT RELATED =====

    override suspend fun getFacilitiesByClient(clientId: String): Result<List<Facility>> {
        return try {
            val entities = facilityDao.getFacilitiesForClient(clientId) // Nome DAO corretto
            val facilities = facilityMapper.toDomainList(entities)
            Result.success(facilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getFacilitiesByClientFlow(clientId: String): Flow<List<Facility>> {
        return facilityDao.getFacilitiesForClientFlow(clientId).map { entities -> // Nome DAO corretto
            facilityMapper.toDomainList(entities)
        }
    }

    override suspend fun getActiveFacilitiesByClient(clientId: String): Result<List<Facility>> {
        return try {
            val entities = facilityDao.getFacilitiesForClient(clientId) // Già filtrato per is_active
            val facilities = facilityMapper.toDomainList(entities)
            Result.success(facilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPrimaryFacility(clientId: String): Result<Facility?> {
        return try {
            val entity = facilityDao.getPrimaryFacility(clientId)
            val facility = entity?.let { facilityMapper.toDomain(it) }
            Result.success(facility)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== FLOW OPERATIONS (REACTIVE) =====

    override fun getAllActiveFacilitiesFlow(): Flow<List<Facility>> {
        // FacilityDao non ha questo metodo, implemento con polling
        return flow {
            while (true) {
                try {
                    val facilities = getActiveFacilities().getOrThrow()
                    emit(facilities)
                    delay(1000)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }
        }
    }

    override fun getFacilityByIdFlow(id: String): Flow<Facility?> {
        return facilityDao.getFacilityByIdFlow(id).map { entity ->
            entity?.let { facilityMapper.toDomain(it) }
        }
    }

    // ===== SEARCH & FILTER =====

    override suspend fun searchFacilities(query: String): Result<List<Facility>> {
        return try {
            val entities = facilityDao.searchFacilities(query)
            val facilities = facilityMapper.toDomainList(entities)
            Result.success(facilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFacilitiesByType(facilityType: FacilityType): Result<List<Facility>> {
        return try {
            val entities = facilityDao.getFacilitiesByType(facilityType.name)
            val facilities = facilityMapper.toDomainList(entities)
            Result.success(facilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== VALIDATION =====

    override suspend fun isFacilityNameTakenForClient(
        clientId: String,
        name: String,
        excludeId: String
    ): Result<Boolean> {
        return try {
            val isTaken = facilityDao.isFacilityNameTakenForClient(clientId, name, excludeId)
            Result.success(isTaken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasPrimaryFacility(clientId: String, excludeId: String): Result<Boolean> {
        return try {
            val primaryFacility = facilityDao.getPrimaryFacility(clientId)
            val hasPrimary = primaryFacility != null && primaryFacility.id != excludeId
            Result.success(hasPrimary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== STATISTICS =====

    override suspend fun getActiveFacilitiesCount(): Result<Int> {
        return try {
            val count = facilityDao.getActiveFacilitiesCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFacilitiesCountByClient(clientId: String): Result<Int> {
        return try {
            val count = facilityDao.getFacilitiesCountForClient(clientId) // Nome DAO corretto
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIslandsCount(facilityId: String): Result<Int> {
        return try {
            val count = facilityDao.getIslandsCountForFacility(facilityId) // Nome DAO corretto
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllFacilityTypes(): Result<List<FacilityType>> {
        return try {
            val typeStrings = facilityDao.getAllFacilityTypes()
            val facilityTypes = typeStrings.mapNotNull { typeString ->
                try {
                    FacilityType.valueOf(typeString)
                } catch (e: IllegalArgumentException) {
                    null // Ignora tipi non validi
                }
            }
            Result.success(facilityTypes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== COMPLEX QUERIES =====

    override suspend fun getFacilitiesWithIslands(): Result<List<Facility>> {
        return try {
            val entities = facilityDao.getFacilitiesWithIslands()
            val facilities = facilityMapper.toDomainList(entities)
            Result.success(facilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== BULK OPERATIONS =====

    override suspend fun createFacilities(facilities: List<Facility>): Result<Unit> {
        return try {
            val entities = facilityMapper.toEntityList(facilities)
            facilityDao.insertFacilities(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setPrimaryFacility(clientId: String, facilityId: String): Result<Unit> {
        return try {
            facilityDao.setPrimaryFacility(clientId, facilityId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== MAINTENANCE =====

    override suspend fun touchFacility(id: String): Result<Unit> {
        return try {
            facilityDao.touchFacility(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}