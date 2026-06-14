@file:Suppress("HardCodedStringLiteral", "unused")

package net.calvuz.qreport.client.contact.domain.model

import kotlinx.serialization.Serializable
import net.calvuz.qreport.app.error.presentation.UiText

/**
 * Statistiche dettagliate per i contatti di un cliente
 * Utilizzate nella scheda ContactsError del ClientDetailScreen
 */
@Serializable
data class ContactStatistics(
    // ===== CONTATORI BASE =====
    val totalContacts: Int,
    val activeContacts: Int,
    val inactiveContacts: Int,
    val primaryContacts: Int,

    // ===== METODI DI CONTATTO =====
    val contactsWithPhone: Int,
    val contactsWithMobile: Int,
    val contactsWithEmail: Int,
    val contactsWithoutContact: Int, // Senza telefono/email

    // ===== ORGANIZZAZIONE =====
    val departmentDistribution: Map<String, Int>, // Dipartimento -> Count
    val roleDistribution: Map<String, Int>, // Ruolo -> Count

    // ===== METODI PREFERITI =====
    val preferredMethodDistribution: Map<UiText, Int>, // Metodo preferito -> Count

    // ===== COMPLETEZZA PROFILI =====
    val completeProfiles: Int, // Con nome, cognome, ruolo, contatto
    val incompleteProfiles: Int
) {

    /**
     * Percentuale contatti attivi
     */
    val activePercentage: Float
        get() = if (totalContacts > 0) (activeContacts.toFloat() / totalContacts) * 100 else 0f

    /**
     * Percentuale contatti con almeno un metodo di contatto
     */
    val contactablePercentage: Float
        get() = if (totalContacts > 0) ((totalContacts - contactsWithoutContact).toFloat() / totalContacts) * 100 else 0f

    /**
     * Percentuale profili completi
     */
    val completenessPercentage: Float
        get() = if (totalContacts > 0) (completeProfiles.toFloat() / totalContacts) * 100 else 0f

    /**
     * Dipartimento più comune
     */
    val topDepartment: String?
        get() = departmentDistribution.maxByOrNull { it.value }?.key?.takeIf { it.isNotBlank() }

    /**
     * Ruolo più comune
     */
    val topRole: String?
        get() = roleDistribution.maxByOrNull { it.value }?.key?.takeIf { it.isNotBlank() }

    companion object {
        /**
         * Crea statistiche vuote per casi di errore
         */
        fun empty() = ContactStatistics(
            totalContacts = 0,
            activeContacts = 0,
            inactiveContacts = 0,
            primaryContacts = 0,
            contactsWithPhone = 0,
            contactsWithMobile = 0,
            contactsWithEmail = 0,
            contactsWithoutContact = 0,
            departmentDistribution = emptyMap(),
            roleDistribution = emptyMap(),
            preferredMethodDistribution = emptyMap(),
            completeProfiles = 0,
            incompleteProfiles = 0
        )
    }
}