package net.calvuz.qreport.domain.model.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.ClientInfo // ← Import esistente per compatibilità

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
     * Conversione a ClientInfo per compatibilità con CheckUp esistente
     */
    fun toClientInfo(
        primaryContact: Contact? = null,
        primaryFacility: Facility? = null
    ): ClientInfo = ClientInfo(
        companyName = companyName,
        contactPerson = primaryContact?.fullName ?: "",
        site = primaryFacility?.name ?: "",
        address = headquarters?.toDisplayString() ?: "",
        phone = primaryContact?.phone ?: "",
        email = primaryContact?.email ?: ""
    )

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
     * Descrizione completa con settore
     */
    val fullDescription: String
        get() = buildString {
            append(companyName)
            if (!industry.isNullOrBlank()) {
                append(" - $industry")
            }
        }

    /**
     * Indirizzo sede legale formattato
     */
    val headquartersAddress: String?
        get() = headquarters?.toDisplayString()

    /**
     * Verifica se cliente ha dati completi
     */
    fun isComplete(): Boolean = companyName.isNotBlank()

    /**
     * Verifica se cliente è operativo (attivo con stabilimenti)
     */
    fun isOperational(): Boolean = isActive && hasFacilities()

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

    /**
     * Statistiche rapide per UI
     */
    fun getQuickStats(
        facilitiesCount: Int = facilities.size,
        contactsCount: Int = contacts.size,
        islandsCount: Int = 0,
        checkupsCount: Int = 0
    ): ClientQuickStats = ClientQuickStats(
        facilitiesCount = facilitiesCount,
        contactsCount = contactsCount,
        islandsCount = islandsCount,
        checkupsCount = checkupsCount
    )
}

/**
 * Badge di stato per UI
 */
@Serializable
data class ClientStatusBadge(
    val text: String,
    val color: String
)

/**
 * Statistiche rapide per UI
 */
@Serializable
data class ClientQuickStats(
    val facilitiesCount: Int,
    val contactsCount: Int,
    val islandsCount: Int,
    val checkupsCount: Int
) {

    /**
     * Testo riassuntivo per UI
     */
    val summaryText: String
        get() = buildString {
            val parts = mutableListOf<String>()

            if (facilitiesCount > 0) {
                parts.add("$facilitiesCount stabiliment${if (facilitiesCount == 1) "o" else "i"}")
            }

            if (islandsCount > 0) {
                parts.add("$islandsCount isol${if (islandsCount == 1) "a" else "e"}")
            }

            if (contactsCount > 0) {
                parts.add("$contactsCount referent${if (contactsCount == 1) "e" else "i"}")
            }

            when {
                parts.isEmpty() -> append("Nessun dato")
                parts.size == 1 -> append(parts.first())
                parts.size == 2 -> append("${parts[0]} e ${parts[1]}")
                else -> append("${parts.dropLast(1).joinToString(", ")} e ${parts.last()}")
            }
        }
}

/**
 * Extension functions per facilità d'uso
 */

/**
 * Crea un cliente di base con solo il nome
 */
fun createBasicClient(companyName: String): Client = Client(
    id = java.util.UUID.randomUUID().toString(),
    companyName = companyName,
    isActive = true,
    createdAt = kotlinx.datetime.Clock.System.now(),
    updatedAt = kotlinx.datetime.Clock.System.now()
)

/**
 * Crea un cliente completo per test/demo
 */
fun createSampleClient(
    companyName: String,
    industry: String? = null,
    city: String? = null
): Client = Client(
    id = java.util.UUID.randomUUID().toString(),
    companyName = companyName,
    industry = industry,
    headquarters = city?.let {
        Address(city = it, country = "Italia")
    },
    isActive = true,
    createdAt = kotlinx.datetime.Clock.System.now(),
    updatedAt = kotlinx.datetime.Clock.System.now()
)