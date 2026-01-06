package net.calvuz.qreport.client.client.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.app.app.domain.model.Address

/**
 * Cliente industriale completo
 * Estende il concetto di ClientInfo esistente mantenendo compatibilità
 */
@Serializable
data class Client(
    val id: String,

    // ===== DATI AZIENDALI =====
    val companyName: String,
    val vatNumber: String? = null,
    val fiscalCode: String? = null,
    val website: String? = null,
    val industry: String? = null,
    val notes: String? = null,

    // ===== LOCALIZZAZIONE =====
    val headquarters: Address? = null,

    // ===== RELAZIONI =====
    val facilities: List<String> = emptyList(),  // IDs delle facilities
    val contacts: List<String> = emptyList(),    // IDs dei contacts

    // ===== METADATI =====
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Verifica se cliente ha stabilimenti
     */
    fun hasFacilities(): Boolean = facilities.isNotEmpty()

    /**
     * Verifica se cliente ha referenti
     */
    fun hasContacts(): Boolean = contacts.isNotEmpty()

    /**
     * Nome display per UI
     */
    val displayName: String
        get() = companyName

    /**
     * Badge per UI basato su stato
     */
    val statusBadge: ClientStatusBadge
        get() = when {
            !isActive -> ClientStatusBadge("Inattivo", "FF0000")
            !hasFacilities() -> ClientStatusBadge("Setup incompleto", "FFC000")
            !hasContacts() -> ClientStatusBadge("Manca referente", "FF9500")
            else -> ClientStatusBadge("Attivo", "00B050")
        }
}
