package net.calvuz.qreport.domain.model.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Stabilimento del cliente
 * Un cliente può avere più stabilimenti produttivi
 */
@Serializable
data class Facility(
    val id: String,
    val clientId: String,

    // ===== DATI STABILIMENTO =====
    val name: String,
    val code: String? = null,              // Codice interno cliente
    val description: String? = null,
    val facilityType: FacilityType = FacilityType.PRODUCTION,

    // ===== LOCALIZZAZIONE =====
    val address: Address,

    // ===== STATO =====
    val isPrimary: Boolean = false,        // Stabilimento principale
    val isActive: Boolean = true,

    // ===== RELAZIONI =====
    val islands: List<String> = emptyList(), // IDs delle isole POLY

    // ===== METADATI =====
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Verifica se ha isole associate
     */
    fun hasIslands(): Boolean = islands.isNotEmpty()

    /**
     * Genera descrizione completa
     */
    val displayName: String
        get() = if (code.isNullOrBlank()) name else "$name ($code)"

    /**
     * Descrizione completa con tipo
     */
    val fullDescription: String
        get() = buildString {
            append(displayName)
            if (!description.isNullOrBlank()) {
                append(" - $description")
            }
            append(" [${facilityType.displayName}]")
        }

    /**
     * Indirizzo formattato per display
     */
    val addressDisplay: String
        get() = address.toDisplayString()

    /**
     * Verifica se lo stabilimento ha dati completi
     */
    fun isComplete(): Boolean = name.isNotBlank() && address.isComplete()

    /**
     * Badge per UI (primario/secondario)
     */
    val badgeText: String?
        get() = when {
            isPrimary -> "Principale"
            !isActive -> "Inattivo"
            else -> null
        }

    /**
     * Colore badge per UI
     */
    val badgeColor: String
        get() = when {
            isPrimary -> "00B050"      // Verde
            !isActive -> "FF0000"      // Rosso
            else -> "6C757D"           // Grigio neutro
        }
}

/**
 * Tipologie di stabilimento
 */
@Serializable
enum class FacilityType(val displayName: String, val description: String) {
    PRODUCTION("Produzione", "Stabilimento produttivo principale"),
    WAREHOUSE("Magazzino", "Deposito e stoccaggio merci"),
    ASSEMBLY("Assemblaggio", "Linea di assemblaggio e montaggio"),
    TESTING("Test e Collaudi", "Laboratorio prove e certificazioni"),
    LOGISTICS("Logistica", "Hub distributivo e spedizioni"),
    OFFICE("Uffici", "Sede amministrativa e direzionale"),
    MAINTENANCE("Manutenzione", "Centro assistenza e riparazioni"),
    R_AND_D("Ricerca e Sviluppo", "Laboratorio ricerca e prototipazione"),
    OTHER("Altro", "Tipologia personalizzata")
}