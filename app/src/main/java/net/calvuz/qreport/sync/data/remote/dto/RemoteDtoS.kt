package net.calvuz.qreport.sync.data.remote.dto

import kotlinx.serialization.Serializable
import net.calvuz.qreport.client.document.sync.remote.DocumentDto

// ===== AUTH =====

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val role: String = "TECHNICIAN"
)

// ===== SYNC =====

@Serializable
data class IslandTypeDto(
    val id: String,
    val code: String,
    val label: String,
    val description: String? = null,
    val iconName: String? = null,
    val maintenanceIntervalDays: Int = 180,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class SyncPayloadDto(
    val deviceId: String,
    val syncTimestamp: Long,
    val islandTypes: List<IslandTypeDto> = emptyList(),
    val clients: List<ClientDto> = emptyList(),
    val contacts: List<ContactDto> = emptyList(),
    val contracts: List<ContractDto> = emptyList(),
    val facilities: List<FacilityDto> = emptyList(),
    val facilityIslands: List<FacilityIslandDto> = emptyList(),
    val mechanicalUnits: List<MechanicalUnitDto> = emptyList(),
    val maintenanceLogs: List<MaintenanceLogDto> = emptyList(),
    val documents: List<DocumentDto> = emptyList(),
    // checkup master data
    val moduleTypes: List<ModuleTypeDto> = emptyList(),
    val criticalityLevels: List<CriticalityLevelDto> = emptyList(),
    val checkupStatuses: List<CheckUpStatusDto> = emptyList(),
    val checkItemTemplates: List<CheckItemTemplateDto> = emptyList(),
    // checkup records
    val checkups: List<CheckUpRecordDto> = emptyList(),
    val checkupIslandAssociations: List<CheckUpIslandAssociationDto> = emptyList()
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
    val islandTypeId: String? = null,
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

@Serializable
data class MaintenanceLogDto(
    val id: String,
    val islandId: String,
    val operationType: String,
    val customOperationLabel: String? = null,
    val mechanicalUnitId: String? = null,
    val componentLabel: String? = null,
    val description: String,
    val technicianName: String,
    val technicianCompany: String? = null,
    val operatingHoursAtEvent: Int? = null,
    val cycleCountAtEvent: Long? = null,
    val outcome: String,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val performedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false
)

// ===== CHECKUP MASTER DTOs =====

@Serializable
data class ModuleTypeDto(
    val id: String,
    val code: String,
    val label: String,
    val description: String? = null,
    val iconName: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class CriticalityLevelDto(
    val id: String,
    val code: String,
    val label: String,
    val priority: Int,
    val colorHex: String,
    val iconEmoji: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class CheckUpStatusDto(
    val id: String,
    val code: String,
    val label: String,
    val colorHex: String,
    val iconEmoji: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val blocksDeletion: Boolean = false,
    val marksCompletion: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class CheckItemTemplateDto(
    val id: String,
    val moduleTypeId: String,
    val category: String,
    val description: String,
    val criticalityId: String,
    val orderIndex: Int,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class IslandDocumentDto(
    val id: String,
    val scope: String,
    val islandId: String? = null,
    val facilityId: String? = null,
    val clientId: String? = null,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val fileHash: String? = null,
    val title: String,
    val category: String,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false
)

@Serializable
data class CheckUpRecordDto(
    val id: String,
    val clientCompanyName: String,
    val clientContactPerson: String = "",
    val clientSite: String = "",
    val clientAddress: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val islandSerialNumber: String = "",
    val islandModel: String = "",
    val islandInstallationDate: String = "",
    val islandLastMaintenanceDate: String = "",
    val islandOperatingHours: Int = 0,
    val islandCycleCount: Long = 0,
    val technicianName: String = "",
    val technicianCompany: String = "",
    val technicianCertification: String = "",
    val technicianPhone: String = "",
    val technicianEmail: String = "",
    val checkupDate: Long,
    val headerNotes: String = "",
    val islandType: String = "",
    val islandTypeId: String? = null,
    val status: String = "DRAFT",
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class CheckUpIslandAssociationDto(
    val id: String,
    val checkupId: String,
    val islandId: String,
    val associationType: String,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null
)
