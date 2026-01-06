package net.calvuz.qreport.client.contact.domain.model

import kotlinx.serialization.Serializable

/**
 * Metodi di contatto preferiti
 */
@Serializable
enum class ContactMethod(val displayName: String) {
    PHONE("Telefono fisso"),
    MOBILE("Cellulare"),
    EMAIL("Email")
}