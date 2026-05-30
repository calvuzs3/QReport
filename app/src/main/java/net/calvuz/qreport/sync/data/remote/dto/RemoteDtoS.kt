package net.calvuz.qreport.sync.data.remote.dto

import kotlinx.serialization.Serializable

// ===== AUTH =====

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String
)

// ===== SYNC =====

@Serializable
data class SyncPayloadDto(
    val deviceId: String,
    val syncTimestamp: Long,
    val clients: List<ClientDto> = emptyList(),
    val contacts: List<ContactDto> = emptyList(),
    val contracts: List<ContractDto> = emptyList(),
    val facilities: List<FacilityDto> = emptyList(),
    val facilityIslands: List<FacilityIslandDto> = emptyList(),
    val mechanicalUnits: List<MechanicalUnitDto> = emptyList()
)

@Serializable
data class SyncResponseDto(
    val acceptedIds: List<String>,
    val pulledPayload: SyncPayloadDto
)

// ===== ENTITY DTOs — mirror of server SyncDto.kt =====

@Serializable
data class ClientDto(
    val id: String,
    val companyName: String,
    val notes: String? = null,
    val headquartersJson: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class ContactDto(
    val id: String,
    val clientId: String,
    val firstName: String,
    val lastName: String? = null,
    val title: String? = null,
    val role: String? = null,
    val department: String? = null,
    val phone: String? = null,
    val mobilePhone: String? = null,
    val email: String? = null,
    val alternativeEmail: String? = null,
    val isPrimary: Boolean = false,
    val preferredContactMethod: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class ContractDto(
    val id: String,
    val clientId: String,
    val name: String? = null,
    val description: String? = null,
    val startDate: Long,
    val endDate: Long,
    val hasPriority: Boolean = true,
    val hasRemoteAssistance: Boolean = true,
    val hasMaintenance: Boolean = true,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class FacilityDto(
    val id: String,
    val clientId: String,
    val name: String,
    val code: String? = null,
    val notes: String? = null,
    val facilityType: String,
    val addressJson: String? = null,
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class FacilityIslandDto(
    val id: String,
    val facilityId: String,
    val commissioningNumber: String? = null,
    val islandType: String,
    val serialNumber: String,
    val modelNumber: String? = null,
    val model: String? = null,
    val installationDate: Long? = null,
    val warrantyExpiration: Long? = null,
    val operatingHours: Int = 0,
    val cycleCount: Long = 0,
    val lastMaintenanceDate: Long? = null,
    val nextScheduledMaintenance: Long? = null,
    val customName: String? = null,
    val location: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class MechanicalUnitDto(
    val id: String,
    val islandId: String,
    val unitType: String,
    val name: String,
    val serialNumber: String? = null,
    val model: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)