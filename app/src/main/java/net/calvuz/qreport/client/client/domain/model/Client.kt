package net.calvuz.qreport.client.client.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.app.app.domain.model.Address

/**
 * Customer
 */
@Serializable
data class Client(

    // ID)
    val id: String,

    // ===== DATA =====
    val companyName: String,
//    val vatNumber: String? = null,
//    val fiscalCode: String? = null,
//    val website: String? = null,
//    val industry: String? = null,
    val notes: String? = null,

    // ===== LOCALIZATION =====
    val headquarters: Address? = null,           // Address in JSON

    // ===== RELATIONSHIPS =====
    val facilities: List<String> = emptyList(),  // Facility IDs
    val contacts: List<String> = emptyList(),    // Contact IDs
    val contracts: List<String> = emptyList(),   // Contract IDs

    // ===== METADATA =====
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) {

    // Helper functions
    fun hasFacilities(): Boolean = facilities.isNotEmpty()
    fun hasContacts(): Boolean = contacts.isNotEmpty()
    fun hasContracts(): Boolean = contracts.isNotEmpty()

    // Helper values
    val displayName: String
        get() = companyName
    val statusBadge: ClientStatusBadge
        get() = when {
            !isActive -> ClientStatusBadge("Inattivo", "FF0000")
//            !hasFacilities() -> ClientStatusBadge("Setup incompleto", "FFC000")
//            !hasContacts() -> ClientStatusBadge("Manca referente", "FF9500")
            else -> ClientStatusBadge("", "00B050")
        }
}
