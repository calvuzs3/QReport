package net.calvuz.qreport.app.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Indirizzo e localizzazione geografica
 * Supporta coordinate GPS per mapping industriale
 */
@Serializable
data class Address(
    // ===== INDIRIZZO =====
    val street: String? = null,
    val streetNumber: String? = null,
    val city: String? = null,
    val province: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String = "Italia",

    // ===== COORDINATE GPS =====
    val coordinates: GeoCoordinates? = null,

    // ===== DETTAGLI AGGIUNTIVI =====
    val notes: String? = null            // Indicazioni aggiuntive per raggiungere
) {

    /**
     * Indirizzo formattato per display
     */
    fun toDisplayString(): String = buildString {
        if (!street.isNullOrBlank()) {
            append(street)
            if (!streetNumber.isNullOrBlank()) {
                append(" $streetNumber")
            }
        }

        if (!city.isNullOrBlank()) {
            if (isNotEmpty()) append(", ")
            append(city)
        }

        if (!postalCode.isNullOrBlank()) {
            if (isNotEmpty()) append(" ")
            append("($postalCode)")
        }

        if (!province.isNullOrBlank()) {
            if (isNotEmpty()) append(" - ")
            append(province)
        }
    }

    /**
     * Verifica se indirizzo è completo
     */
    fun isComplete(): Boolean = !street.isNullOrBlank() && !city.isNullOrBlank()

    fun hasCoordinates(): Boolean = false
}