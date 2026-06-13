package net.calvuz.qreport.client.document.sync.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for [net.calvuz.qreport.client.document.domain.model.Document]
 * in the entity sync JSON payload.
 *
 * Note: [filePath] is intentionally absent — paths are local to each device.
 * Each device resolves its own path via [DocumentDirectories.forScope()] after
 * receiving the metadata. The bytes travel through the separate file sync channel.
 *
 * [fileHash] is included so the receiving device knows whether it needs to
 * download the file or already has an identical copy.
 */
@Serializable
data class DocumentDto(
    val id: String,
    val scope: String,

    @SerialName("island_id")   val islandId: String?   = null,
    @SerialName("facility_id") val facilityId: String? = null,
    @SerialName("client_id")   val clientId: String?   = null,

    @SerialName("file_name")   val fileName: String,
    @SerialName("file_size")   val fileSize: Long,
    @SerialName("mime_type")   val mimeType: String,
    @SerialName("file_hash")   val fileHash: String?   = null,

    val title: String,
    val category: String,
    val notes: String?         = null,

    @SerialName("created_at")  val createdAt: Long,
    @SerialName("updated_at")  val updatedAt: Long,
    @SerialName("is_active")   val isActive: Boolean,
    @SerialName("is_deleted")  val isDeleted: Boolean,
    @SerialName("synced_at")   val syncedAt: Long?     = null
)

// ─────────────────────────────────────────────────────────────────────────────
// FILE: sync/data/remote/dto/RemoteDtos.kt
//
// ADD `documents` field to the existing SyncPayload:
//
//     @Serializable
//     data class SyncPayload(
//         val deviceId: String,
//         val syncTimestamp: Long,
//         val clients: List<ClientDto> = emptyList(),
//         val contacts: List<ContactDto> = emptyList(),
//         val contracts: List<ContractDto> = emptyList(),
//         val facilities: List<FacilityDto> = emptyList(),
//         val facilityIslands: List<IslandDto> = emptyList(),
//         val mechanicalUnits: List<MechanicalUnitDto> = emptyList(),
//         val documents: List<DocumentDto> = emptyList()   // ← ADD
//     )
//
// The default emptyList() ensures backward compatibility with server
// versions that don't yet support the documents field.
// ─────────────────────────────────────────────────────────────────────────────